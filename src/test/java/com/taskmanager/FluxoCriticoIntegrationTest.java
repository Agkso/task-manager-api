package com.taskmanager;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cobre o fluxo critico ponta a ponta: registrar -> logar -> criar projeto ->
 * criar tarefa -> mover pra IN_PROGRESS -> conferir no relatorio. Sobe um
 * Postgres real via Testcontainers pra nao mascarar diferenca de dialeto
 * (o resto dos testes de servico usa Mockito e nao toca banco).
 *
 * @ActiveProfiles("test") ativa application-test.yml (rate-limit alto) - ver
 * o comentario la para o motivo (MockMvc + contexto Spring compartilhado
 * entre classes de teste).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FluxoCriticoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registrarLoginCriarProjetoCriarTarefaEMudarStatus() throws Exception {
        String corpoRegistro = objectMapper.writeValueAsString(
                Map.of("nome", "Ana Dev", "email", "ana.integracao@example.com", "senha", "senha1234"));

        String respostaRegistro = mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRegistro))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(respostaRegistro).get("token").asText();

        String corpoProjeto =
                objectMapper.writeValueAsString(Map.of("nome", "Projeto Integracao", "descricao", "teste"));
        String respostaProjeto = mockMvc.perform(post("/api/projetos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoProjeto))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long projetoId = objectMapper.readTree(respostaProjeto).get("id").asLong();

        String corpoTarefa = objectMapper.writeValueAsString(Map.of("titulo", "Tarefa 1", "prioridade", "HIGH"));
        String respostaTarefa = mockMvc.perform(post("/api/projetos/{id}/tarefas", projetoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoTarefa))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("TODO")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long tarefaId = objectMapper.readTree(respostaTarefa).get("id").asLong();

        String corpoStatus = objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"));
        mockMvc.perform(patch("/api/projetos/{projetoId}/tarefas/{tarefaId}/status", projetoId, tarefaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoStatus))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        mockMvc.perform(get("/api/projetos/{id}/tarefas/relatorio", projetoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS", is(1)));
    }

    @Test
    void requisicaoSemToken_deveRetornar401() throws Exception {
        mockMvc.perform(get("/api/projetos")).andExpect(status().isUnauthorized());
    }
}
