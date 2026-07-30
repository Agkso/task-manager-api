package com.taskmanager.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc nao sai da JVM: toda requisicao chega com o mesmo
 * request.getRemoteAddr() ("127.0.0.1"), entao o bucket do
 * FiltroLimitacaoRequisicoes e compartilhado entre todos os testes que usam
 * o mesmo contexto Spring. Os outros testes de integracao (FluxoCritico,
 * RelatorioCache, ProjetoAutorizacao) ativam o profile "test"
 * (application-test.yml), que sobe o rate-limit padrao pra nao esbarrar
 * nisso. Aqui e' o oposto: o @TestPropertySource fixa o limite baixo de
 * proposito - e' exatamente o que este teste quer validar - e, por ter
 * propriedades diferentes dos outros, força o Spring a criar um contexto
 * separado pra essa classe (a chave de cache de contexto inclui as
 * propriedades ativas). @DirtiesContext depois da classe e uma segunda
 * camada de seguranca, caso outra classe algum dia reuse essas mesmas
 * propriedades.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"app.rate-limit.capacidade=5", "app.rate-limit.janela-minutos=1"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FiltroLimitacaoRequisicoesIntegrationTest {

    private static final int CAPACIDADE_PADRAO = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_deveBloquearAposExcederLimiteDeTentativasPorIp() throws Exception {
        String corpoLoginInvalido =
                objectMapper.writeValueAsString(Map.of("email", "inexistente@example.com", "senha", "qualquer123"));

        for (int tentativa = 0; tentativa < CAPACIDADE_PADRAO; tentativa++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoLoginInvalido))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLoginInvalido))
                .andExpect(status().isTooManyRequests())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                                result.getResponse().getHeader("Retry-After"))
                        .isNotNull());
    }
}
