package com.taskmanager.project;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Cobre membership e autorizacao (ADMIN vs MEMBER) nos endpoints de projeto
 * de ponta a ponta - o unico caminho ja coberto por integracao antes disso
 * era o fluxo critico de tarefa (FluxoCriticoIntegrationTest). Sobe Postgres
 * real via Testcontainers pelo mesmo motivo daquele teste: essas regras
 * (quem pode atualizar/excluir projeto, adicionar/remover membro) sao
 * autorizacao de verdade, e um teste de unidade com mock ja cobre a logica
 * isolada (ver MembroProjetoServiceTest) - o que falta e garantir que o
 * controller/security chain expõe isso corretamente via HTTP.
 *
 * @ActiveProfiles("test") ativa application-test.yml (rate-limit alto) - essa
 * classe sozinha registra mais de uma dezena de usuarios via /api/auth/registrar,
 * o que estouraria o limite padrao (5/min) sem isso.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjetoAutorizacaoIntegrationTest {

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

    private long criarProjeto(String tokenDono, String nome) throws Exception {
        String corpo = objectMapper.writeValueAsString(Map.of("nome", nome, "descricao", "teste"));
        String resposta = mockMvc.perform(post("/api/projetos")
                        .header("Authorization", "Bearer " + tokenDono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(resposta).get("id").asLong();
    }

    private long adicionarMembro(String tokenSolicitante, long projetoId, String email, String papel)
            throws Exception {
        String corpo = objectMapper.writeValueAsString(Map.of("email", email, "papel", papel));
        String resposta = mockMvc.perform(post("/api/projetos/{id}/membros", projetoId)
                        .header("Authorization", "Bearer " + tokenSolicitante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(resposta).get("usuarioId").asLong();
    }

    @Test
    void member_naoPodeAdicionarOutroMembro() throws Exception {
        String tokenDono = registrarEObterToken("Dono", "dono1@example.com");
        String tokenMember = registrarEObterToken("Membro", "membro1@example.com");
        long projetoId = criarProjeto(tokenDono, "Projeto 1");
        adicionarMembro(tokenDono, projetoId, "membro1@example.com", "MEMBER");

        String corpoNovoMembro =
                objectMapper.writeValueAsString(Map.of("email", "outro1@example.com", "papel", "MEMBER"));
        mockMvc.perform(post("/api/projetos/{id}/membros", projetoId)
                        .header("Authorization", "Bearer " + tokenMember)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoNovoMembro))
                .andExpect(status().isForbidden());
    }

    @Test
    void member_naoPodeAtualizarProjeto() throws Exception {
        String tokenDono = registrarEObterToken("Dono", "dono2@example.com");
        String tokenMember = registrarEObterToken("Membro", "membro2@example.com");
        long projetoId = criarProjeto(tokenDono, "Projeto 2");
        adicionarMembro(tokenDono, projetoId, "membro2@example.com", "MEMBER");

        String corpoAtualizacao =
                objectMapper.writeValueAsString(Map.of("nome", "Projeto 2 renomeado", "descricao", "nova desc"));
        mockMvc.perform(put("/api/projetos/{id}", projetoId)
                        .header("Authorization", "Bearer " + tokenMember)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAtualizacao))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_podeAtualizarProjeto() throws Exception {
        String tokenDono = registrarEObterToken("Dono", "dono3@example.com");
        long projetoId = criarProjeto(tokenDono, "Projeto 3");

        String corpoAtualizacao =
                objectMapper.writeValueAsString(Map.of("nome", "Projeto 3 renomeado", "descricao", "nova desc"));
        mockMvc.perform(put("/api/projetos/{id}", projetoId)
                        .header("Authorization", "Bearer " + tokenDono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAtualizacao))
                .andExpect(status().isOk());
    }

    @Test
    void naoMembro_naoPodeVerProjetoNemListarMembros() throws Exception {
        String tokenDono = registrarEObterToken("Dono", "dono4@example.com");
        String tokenForasteiro = registrarEObterToken("Forasteiro", "forasteiro4@example.com");
        long projetoId = criarProjeto(tokenDono, "Projeto 4");

        mockMvc.perform(get("/api/projetos/{id}", projetoId).header("Authorization", "Bearer " + tokenForasteiro))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/projetos/{id}/membros", projetoId)
                        .header("Authorization", "Bearer " + tokenForasteiro))
                .andExpect(status().isForbidden());
    }

    @Test
    void member_naoPodeRemoverMembro_masAdminPode() throws Exception {
        String tokenDono = registrarEObterToken("Dono", "dono5@example.com");
        String tokenMember = registrarEObterToken("Membro", "membro5@example.com");
        registrarEObterToken("Terceiro", "terceiro5@example.com");
        long projetoId = criarProjeto(tokenDono, "Projeto 5");
        adicionarMembro(tokenDono, projetoId, "membro5@example.com", "MEMBER");
        long terceiroId = adicionarMembro(tokenDono, projetoId, "terceiro5@example.com", "MEMBER");

        mockMvc.perform(delete("/api/projetos/{id}/membros/{usuarioId}", projetoId, terceiroId)
                        .header("Authorization", "Bearer " + tokenMember))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/projetos/{id}/membros/{usuarioId}", projetoId, terceiroId)
                        .header("Authorization", "Bearer " + tokenDono))
                .andExpect(status().isNoContent());
    }

    @Test
    void naoPodeRemoverDonoDosMembrosMesmoSendoAdmin() throws Exception {
        String tokenDono = registrarEObterToken("Dono", "dono6@example.com");
        long projetoId = criarProjeto(tokenDono, "Projeto 6");

        String respostaMembros = mockMvc.perform(
                        get("/api/projetos/{id}/membros", projetoId).header("Authorization", "Bearer " + tokenDono))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long donoId = objectMapper.readTree(respostaMembros).get(0).get("usuarioId").asLong();

        mockMvc.perform(delete("/api/projetos/{id}/membros/{usuarioId}", projetoId, donoId)
                        .header("Authorization", "Bearer " + tokenDono))
                .andExpect(status().isConflict());
    }

    @Test
    void apenasODono_naoOutroAdmin_podeExcluirProjeto() throws Exception {
        String tokenDono = registrarEObterToken("Dono", "dono7@example.com");
        String tokenOutroAdmin = registrarEObterToken("OutroAdmin", "outroadmin7@example.com");
        long projetoId = criarProjeto(tokenDono, "Projeto 7");
        adicionarMembro(tokenDono, projetoId, "outroadmin7@example.com", "ADMIN");

        mockMvc.perform(delete("/api/projetos/{id}", projetoId).header("Authorization", "Bearer " + tokenOutroAdmin))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/projetos/{id}", projetoId).header("Authorization", "Bearer " + tokenDono))
                .andExpect(status().isNoContent());
    }

    @Test
    void excluir_eSoftDelete_projetoSomeDaListaEBloqueiaAcessoAsTarefas() throws Exception {
        String tokenDono = registrarEObterToken("Dono", "dono8@example.com");
        long projetoId = criarProjeto(tokenDono, "Projeto 8");

        mockMvc.perform(delete("/api/projetos/{id}", projetoId).header("Authorization", "Bearer " + tokenDono))
                .andExpect(status().isNoContent());

        // buscar() checa obterMembro antes de buscarPorId - mesmo comportamento de sempre pra
        // "nao sou membro" (403), so que agora um projeto excluido cai no mesmo caminho
        mockMvc.perform(get("/api/projetos/{id}", projetoId).header("Authorization", "Bearer " + tokenDono))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projetos").header("Authorization", "Bearer " + tokenDono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // obterMembro nega acesso a um projeto excluido como se o solicitante nunca tivesse
        // sido membro - as tarefas continuam no banco (nao cascateadas), mas inalcancaveis
        mockMvc.perform(get("/api/projetos/{id}/tarefas", projetoId).header("Authorization", "Bearer " + tokenDono))
                .andExpect(status().isForbidden());
    }
}
