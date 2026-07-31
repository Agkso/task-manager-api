package com.taskmanager.task;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
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
 * Cobre, via HTTP real (MockMvc + Postgres via Testcontainers), os caminhos
 * de GET /tarefas (filtro + paginacao), DELETE /tarefas/{id} e GET
 * /tarefas/{id}/historico - antes dessa classe, so existia teste unitario
 * (via use case, com repositorio mockado) pra esses tres fluxos; o que
 * faltava era garantir que o controller/@ModelAttribute/serializacao JSON
 * realmente expõem isso corretamente ponta a ponta.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TarefaConsultaIntegrationTest {

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

    private long criarProjeto(String token, String nome) throws Exception {
        String corpo = objectMapper.writeValueAsString(Map.of("nome", nome, "descricao", "teste"));
        String resposta = mockMvc.perform(post("/api/projetos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(resposta).get("id").asLong();
    }

    private long criarTarefa(String token, long projetoId, String titulo, String prioridade) throws Exception {
        String corpo = objectMapper.writeValueAsString(Map.of("titulo", titulo, "prioridade", prioridade));
        String resposta = mockMvc.perform(post("/api/projetos/{id}/tarefas", projetoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(resposta).get("id").asLong();
    }

    private long criarTarefaComResponsavel(String token, long projetoId, String titulo, long responsavelId)
            throws Exception {
        String corpo = objectMapper.writeValueAsString(
                Map.of("titulo", titulo, "prioridade", "MEDIUM", "responsavelId", responsavelId));
        String resposta = mockMvc.perform(post("/api/projetos/{id}/tarefas", projetoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(resposta).get("id").asLong();
    }

    private long obterUsuarioId(String token, long projetoId) throws Exception {
        String resposta = mockMvc.perform(
                        get("/api/projetos/{id}/membros", projetoId).header("Authorization", "Bearer " + token))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(resposta).get(0).get("usuarioId").asLong();
    }

    private void mudarStatus(String token, long projetoId, long tarefaId, String status) throws Exception {
        mockMvc.perform(patch("/api/projetos/{projetoId}/tarefas/{tarefaId}/status", projetoId, tarefaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", status))))
                .andExpect(status().isOk());
    }

    @Test
    void listar_deveFiltrarPorStatus() throws Exception {
        String token = registrarEObterToken("Ana", "ana.listar@example.com");
        long projetoId = criarProjeto(token, "Projeto Listagem");
        long tarefaAlvo = criarTarefa(token, projetoId, "Tarefa em andamento", "HIGH");
        criarTarefa(token, projetoId, "Tarefa parada", "LOW");

        mockMvc.perform(patch("/api/projetos/{projetoId}/tarefas/{tarefaId}/status", projetoId, tarefaAlvo)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/projetos/{id}/tarefas", projetoId)
                        .header("Authorization", "Bearer " + token)
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].id", is((int) tarefaAlvo)))
                .andExpect(jsonPath("$.totalElementos", is(1)));
    }

    @Test
    void listar_devePaginar() throws Exception {
        String token = registrarEObterToken("Bia", "bia.listar@example.com");
        long projetoId = criarProjeto(token, "Projeto Paginacao");
        for (int i = 0; i < 3; i++) {
            criarTarefa(token, projetoId, "Tarefa " + i, "MEDIUM");
        }

        mockMvc.perform(get("/api/projetos/{id}/tarefas", projetoId)
                        .header("Authorization", "Bearer " + token)
                        .param("pagina", "0")
                        .param("tamanho", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(2)))
                .andExpect(jsonPath("$.totalElementos", is(3)))
                .andExpect(jsonPath("$.totalPaginas", is(2)));
    }

    @Test
    void excluir_deveRemoverTarefaEBuscaSeguinteRetorna404() throws Exception {
        String token = registrarEObterToken("Caio", "caio.excluir@example.com");
        long projetoId = criarProjeto(token, "Projeto Exclusao");
        long tarefaId = criarTarefa(token, projetoId, "Tarefa descartavel", "LOW");

        mockMvc.perform(delete("/api/projetos/{projetoId}/tarefas/{tarefaId}", projetoId, tarefaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projetos/{projetoId}/tarefas/{tarefaId}", projetoId, tarefaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void excluir_eSoftDelete_somaDaListagemENaoContaMaisNoLimiteDeWip() throws Exception {
        String token = registrarEObterToken("Elis", "elis.softdelete@example.com");
        long projetoId = criarProjeto(token, "Projeto Soft Delete");
        long usuarioId = obterUsuarioId(token, projetoId);

        long tarefaExcluida = criarTarefaComResponsavel(token, projetoId, "Tarefa a sumir", usuarioId);
        criarTarefa(token, projetoId, "Tarefa que fica", "LOW");
        mudarStatus(token, projetoId, tarefaExcluida, "IN_PROGRESS");

        mockMvc.perform(delete("/api/projetos/{projetoId}/tarefas/{tarefaId}", projetoId, tarefaExcluida)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // soft delete: some da listagem (Specification filtra excluidoEm is null)
        mockMvc.perform(get("/api/projetos/{id}/tarefas", projetoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos", is(1)));

        // a excluida ficou IN_PROGRESS no banco (soft delete nao muda status) - se ela ainda
        // contasse pro limite de WIP (5) do mesmo responsavel, a 5a tarefa nova abaixo falharia
        // com RegraNegocioException (409); todas passarem prova que countBy...ExcluidoEmIsNull
        // filtra ela corretamente
        for (int i = 0; i < 5; i++) {
            long outraTarefaId = criarTarefaComResponsavel(token, projetoId, "WIP " + i, usuarioId);
            mudarStatus(token, projetoId, outraTarefaId, "IN_PROGRESS");
        }
    }

    @Test
    void historico_deveListarMudancasDeStatusDaMaisRecenteParaAMaisAntiga() throws Exception {
        String token = registrarEObterToken("Duda", "duda.historico@example.com");
        long projetoId = criarProjeto(token, "Projeto Historico");
        long tarefaId = criarTarefa(token, projetoId, "Tarefa acompanhada", "MEDIUM");

        mockMvc.perform(patch("/api/projetos/{projetoId}/tarefas/{tarefaId}/status", projetoId, tarefaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/projetos/{projetoId}/tarefas/{tarefaId}/status", projetoId, tarefaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DONE"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/projetos/{projetoId}/tarefas/{tarefaId}/historico", projetoId, tarefaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].statusAnterior", is("IN_PROGRESS")))
                .andExpect(jsonPath("$[0].statusNovo", is("DONE")))
                .andExpect(jsonPath("$[1].statusAnterior", is("TODO")))
                .andExpect(jsonPath("$[1].statusNovo", is("IN_PROGRESS")));
    }

    @Test
    void eventos_membroConectaViaHeaderOuQueryParam() throws Exception {
        String token = registrarEObterToken("Fabio", "fabio.sse@example.com");
        long projetoId = criarProjeto(token, "Projeto SSE");

        mockMvc.perform(get("/api/projetos/{id}/tarefas/eventos", projetoId).header("Authorization", "Bearer " + token))
                .andExpect(request().asyncStarted());

        // EventSource do browser nao manda headers customizados - o fallback via
        // query param (?token=) e' o que permite a conexao real do frontend
        mockMvc.perform(get("/api/projetos/{id}/tarefas/eventos", projetoId).param("token", token))
                .andExpect(request().asyncStarted());
    }

    @Test
    void eventos_naoMembroENaoAutenticado_saoRejeitados() throws Exception {
        String tokenDono = registrarEObterToken("Helo", "helo.sse@example.com");
        String tokenForasteiro = registrarEObterToken("Ivo", "ivo.sse@example.com");
        long projetoId = criarProjeto(tokenDono, "Projeto SSE Privado");

        mockMvc.perform(get("/api/projetos/{id}/tarefas/eventos", projetoId)
                        .header("Authorization", "Bearer " + tokenForasteiro))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projetos/{id}/tarefas/eventos", projetoId)).andExpect(status().isUnauthorized());
    }
}
