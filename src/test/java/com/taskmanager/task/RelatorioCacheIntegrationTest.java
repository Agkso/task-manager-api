package com.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.TestcontainersConfiguration;
import com.taskmanager.config.ConfiguracaoCache;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica o comportamento do cache do relatorio de ponta a ponta (nao da
 * pra testar @Cacheable/@CacheEvict com Mockito puro - sao interceptados
 * por um proxy Spring que so existe com o contexto de verdade no ar).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RelatorioCacheIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    private String token;
    private long projetoId;

    @BeforeEach
    void registrarECriarProjeto() throws Exception {
        String corpoRegistro = objectMapper.writeValueAsString(Map.of(
                "nome",
                "Usuario Cache",
                "email",
                "cache-" + System.nanoTime() + "@example.com",
                "senha",
                "senha1234"));
        String respostaRegistro = mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRegistro))
                .andReturn()
                .getResponse()
                .getContentAsString();
        token = objectMapper.readTree(respostaRegistro).get("token").asText();

        String corpoProjeto =
                objectMapper.writeValueAsString(Map.of("nome", "Projeto Cache", "descricao", "teste"));
        String respostaProjeto = mockMvc.perform(post("/api/projetos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoProjeto))
                .andReturn()
                .getResponse()
                .getContentAsString();
        projetoId = objectMapper.readTree(respostaProjeto).get("id").asLong();
    }

    private void criarTarefa(String titulo) throws Exception {
        String corpoTarefa = objectMapper.writeValueAsString(Map.of("titulo", titulo, "prioridade", "LOW"));
        mockMvc.perform(post("/api/projetos/{id}/tarefas", projetoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoTarefa))
                .andExpect(status().isCreated());
    }

    @Test
    void relatorio_devePopularCacheEEvictarAoCriarNovaTarefa() throws Exception {
        Cache cacheRelatorio = cacheManager.getCache(ConfiguracaoCache.CACHE_RELATORIO_TAREFAS);
        assertThat(cacheRelatorio).isNotNull();

        criarTarefa("Tarefa 1");
        assertThat(cacheRelatorio.get(projetoId)).isNull(); // criar evictou (nada foi lido ainda)

        mockMvc.perform(get("/api/projetos/{id}/tarefas/relatorio", projetoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatus.TODO", org.hamcrest.Matchers.is(1)));
        assertThat(cacheRelatorio.get(projetoId)).isNotNull(); // primeira leitura populou o cache

        criarTarefa("Tarefa 2");
        assertThat(cacheRelatorio.get(projetoId)).isNull(); // segunda escrita evictou de novo

        mockMvc.perform(get("/api/projetos/{id}/tarefas/relatorio", projetoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatus.TODO", org.hamcrest.Matchers.is(2)));
    }
}
