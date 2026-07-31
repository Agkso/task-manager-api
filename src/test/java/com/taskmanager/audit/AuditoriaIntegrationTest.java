package com.taskmanager.audit;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.TestcontainersConfiguration;
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
 * Cobre GET /projetos/{id}/auditoria via HTTP real, provando que
 * AuditoriaListener (AFTER_COMMIT) de fato grava o EventoAuditoria publicado
 * em ProjetoService/MembroProjetoService antes do MockMvc.perform() retornar
 * - a mesma garantia que HistoricoTarefaListener ja tinha, testada aqui pro
 * caminho de auditoria.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registrarEObterToken(String nome, String email) throws Exception {
        String corpo = objectMapper.writeValueAsString(Map.of("nome", nome, "email", email, "senha", "senha1234"));
        String resposta = mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(resposta).get("token").asText();
    }

    @Test
    void auditoria_registraCriacaoDeProjetoEAdicaoDeMembroEConsultaSoParaAdmin() throws Exception {
        String tokenDono = registrarEObterToken("Dono", "dono.auditoria@example.com");
        String tokenMembro = registrarEObterToken("Membro", "membro.auditoria@example.com");

        String corpoProjeto = objectMapper.writeValueAsString(Map.of("nome", "Projeto Auditoria", "descricao", "x"));
        String respostaProjeto = mockMvc.perform(post("/api/projetos")
                        .header("Authorization", "Bearer " + tokenDono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoProjeto))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long projetoId = objectMapper.readTree(respostaProjeto).get("id").asLong();

        String corpoMembro =
                objectMapper.writeValueAsString(Map.of("email", "membro.auditoria@example.com", "papel", "MEMBER"));
        mockMvc.perform(post("/api/projetos/{id}/membros", projetoId)
                        .header("Authorization", "Bearer " + tokenDono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoMembro))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/api/projetos/{id}/auditoria", projetoId).header("Authorization", "Bearer " + tokenDono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.conteudo[?(@.acao == 'PROJETO_CRIADO')]").exists())
                .andExpect(jsonPath("$.conteudo[?(@.acao == 'MEMBRO_ADICIONADO')]").exists());

        mockMvc.perform(get("/api/projetos/{id}/auditoria", projetoId).header("Authorization", "Bearer " + tokenMembro))
                .andExpect(status().isForbidden());
    }
}
