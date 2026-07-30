package com.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.project.enums.Papel;
import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import com.taskmanager.user.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Sem mocks de proposito: RegrasTransicaoStatusTarefa nao depende de
 * repositorio nenhum, entao o teste so monta o objeto e chama validar().
 */
class RegrasTransicaoStatusTarefaTest {

    private final RegrasTransicaoStatusTarefa regras = new RegrasTransicaoStatusTarefa();

    private Tarefa tarefa(StatusTarefa status, Prioridade prioridade) {
        return Tarefa.builder().status(status).prioridade(prioridade).build();
    }

    private Tarefa tarefaComResponsavel(StatusTarefa status, Prioridade prioridade) {
        Usuario responsavel = Usuario.builder().id(1L).nome("Responsavel").email("resp@ex.com").build();
        return Tarefa.builder()
                .status(status)
                .prioridade(prioridade)
                .responsavel(responsavel)
                .build();
    }

    @Test
    void deveRejeitarDoneParaTodo() {
        Tarefa tarefa = tarefa(StatusTarefa.DONE, Prioridade.LOW);

        assertThatThrownBy(() -> regras.validar(tarefa, StatusTarefa.TODO, Papel.ADMIN, 0))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void devePermitirDoneParaInProgress() {
        Tarefa tarefa = tarefa(StatusTarefa.DONE, Prioridade.LOW);

        assertThatCode(() -> regras.validar(tarefa, StatusTarefa.IN_PROGRESS, Papel.MEMBER, 0))
                .doesNotThrowAnyException();
    }

    @Test
    void deveRejeitarMembroComumFechandoCritica() {
        Tarefa tarefa = tarefa(StatusTarefa.IN_PROGRESS, Prioridade.CRITICAL);

        assertThatThrownBy(() -> regras.validar(tarefa, StatusTarefa.DONE, Papel.MEMBER, 0))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void devePermitirAdminFechandoCritica() {
        Tarefa tarefa = tarefa(StatusTarefa.IN_PROGRESS, Prioridade.CRITICAL);

        assertThatCode(() -> regras.validar(tarefa, StatusTarefa.DONE, Papel.ADMIN, 0))
                .doesNotThrowAnyException();
    }

    @Test
    void devePermitirMembroComumFechandoTarefaNaoCritica() {
        Tarefa tarefa = tarefa(StatusTarefa.IN_PROGRESS, Prioridade.HIGH);

        assertThatCode(() -> regras.validar(tarefa, StatusTarefa.DONE, Papel.MEMBER, 0))
                .doesNotThrowAnyException();
    }

    @Test
    void deveRejeitarNoLimiteDeWip() {
        Tarefa tarefa = tarefaComResponsavel(StatusTarefa.TODO, Prioridade.MEDIUM);

        assertThatThrownBy(() -> regras.validar(
                        tarefa,
                        StatusTarefa.IN_PROGRESS,
                        Papel.MEMBER,
                        RegrasTransicaoStatusTarefa.LIMITE_TAREFAS_EM_ANDAMENTO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Limite");
    }

    @Test
    void devePermitirAbaixoDoLimiteDeWip() {
        Tarefa tarefa = tarefaComResponsavel(StatusTarefa.TODO, Prioridade.MEDIUM);

        assertThatCode(() -> regras.validar(
                        tarefa,
                        StatusTarefa.IN_PROGRESS,
                        Papel.MEMBER,
                        RegrasTransicaoStatusTarefa.LIMITE_TAREFAS_EM_ANDAMENTO - 1))
                .doesNotThrowAnyException();
    }

    @Test
    void naoDeveChecarLimiteDeWipQuandoJaEstavaEmAndamento() {
        Tarefa tarefa = tarefaComResponsavel(StatusTarefa.IN_PROGRESS, Prioridade.MEDIUM);

        // ja estava IN_PROGRESS - "mudar" pra IN_PROGRESS de novo e um no-op,
        // nao deve contar como uma nova entrada no limite de WIP
        assertThatCode(() -> regras.validar(
                        tarefa,
                        StatusTarefa.IN_PROGRESS,
                        Papel.MEMBER,
                        RegrasTransicaoStatusTarefa.LIMITE_TAREFAS_EM_ANDAMENTO))
                .doesNotThrowAnyException();
    }
}
