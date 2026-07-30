package com.taskmanager.task;

import com.taskmanager.exception.MensagensErro;
import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.project.enums.Papel;
import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * As 3 regras de transicao de status exigidas pelo enunciado, isoladas do
 * orquestrador (MudarStatusTarefaUseCase) de proposito: sao regras de
 * dominio puras, sem dependencia de repositorio ou de request/response,
 * entao dao pra testar direto (ver RegrasTransicaoStatusTarefaTest) sem
 * precisar mockar nada. MudarStatusTarefaUseCase fica responsavel so por
 * buscar os dados (tarefa atual, contagem de WIP do responsavel) e delegar
 * a decisao pra ca.
 *
 * Com apenas 3 regras fixas, uma lista de "policies" (Strategy/Chain of
 * Responsibility) seria mais aberta a extensao (OCP), mas tambem seria
 * complexidade sem uso real hoje - se o numero de regras crescer, essa e
 * a proxima refatoracao natural.
 */
@Component
public class RegrasTransicaoStatusTarefa {

    static final int LIMITE_TAREFAS_EM_ANDAMENTO = 5;

    public void validar(
            Tarefa tarefa, StatusTarefa novoStatus, Papel papelSolicitante, long tarefasEmAndamentoDoResponsavel) {
        StatusTarefa statusAtual = tarefa.getStatus();

        if (statusAtual == StatusTarefa.DONE && novoStatus == StatusTarefa.TODO) {
            throw new RegraNegocioException(MensagensErro.TRANSICAO_DONE_PARA_TODO_PROIBIDA);
        }

        if (novoStatus == StatusTarefa.DONE
                && tarefa.getPrioridade() == Prioridade.CRITICAL
                && papelSolicitante != Papel.ADMIN) {
            throw new AccessDeniedException(MensagensErro.APENAS_ADMIN_CONCLUI_TAREFA_CRITICAL);
        }

        boolean entrandoEmAndamento =
                novoStatus == StatusTarefa.IN_PROGRESS && statusAtual != StatusTarefa.IN_PROGRESS;
        if (entrandoEmAndamento
                && tarefa.getResponsavel() != null
                && tarefasEmAndamentoDoResponsavel >= LIMITE_TAREFAS_EM_ANDAMENTO) {
            throw new RegraNegocioException(
                    MensagensErro.limiteDeTarefasEmAndamentoAtingido(LIMITE_TAREFAS_EM_ANDAMENTO));
        }
    }
}
