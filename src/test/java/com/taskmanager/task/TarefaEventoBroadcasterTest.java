package com.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class TarefaEventoBroadcasterTest {

    private static final Long PROJETO_ID = 1L;
    private static final Long OUTRO_PROJETO_ID = 2L;

    private final TarefaEventoBroadcaster broadcaster = new TarefaEventoBroadcaster();

    @Test
    void publicar_semInscritos_naoLancaExcecao() {
        assertThatCode(() -> broadcaster.publicar(PROJETO_ID, "status-alterado", "payload"))
                .doesNotThrowAnyException();
    }

    @Test
    void inscrever_deveRegistrarUmEmissorParaOProjeto() {
        broadcaster.inscrever(PROJETO_ID);

        assertThat(broadcaster.quantidadeInscritos(PROJETO_ID)).isEqualTo(1);
    }

    @Test
    void inscrever_naoDeveAfetarContagemDeOutroProjeto() {
        broadcaster.inscrever(PROJETO_ID);

        assertThat(broadcaster.quantidadeInscritos(OUTRO_PROJETO_ID)).isZero();
    }

    @Test
    void publicar_deveRemoverEmissorQuandoOEnvioFalha() {
        SseEmitter emissor = broadcaster.inscrever(PROJETO_ID);
        // completa o emissor "por fora" (simula uma conexao ja encerrada) - o proximo
        // send() dentro de publicar() vai lancar IllegalStateException
        emissor.complete();
        assertThat(broadcaster.quantidadeInscritos(PROJETO_ID)).isEqualTo(1);

        broadcaster.publicar(PROJETO_ID, "status-alterado", "payload");

        assertThat(broadcaster.quantidadeInscritos(PROJETO_ID)).isZero();
    }
}
