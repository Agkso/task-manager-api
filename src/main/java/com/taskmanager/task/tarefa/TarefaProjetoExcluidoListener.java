package com.taskmanager.task.tarefa;

import com.taskmanager.project.projeto.ProjetoExcluidoEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Cascateia o soft delete do projeto para suas tarefas via evento, sem
 * inverter a dependencia (project nunca importa task - task e' quem ouve
 * o evento de project). Mesmo padrao de AFTER_COMMIT + REQUIRES_NEW do
 * HistoricoTarefaListener: nao soft-deleta tarefas de uma exclusao de
 * projeto que sofreu rollback, e abre propria transacao porque a original
 * ja fechou no commit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TarefaProjetoExcluidoListener {

    private final TarefaRepository tarefaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoExcluirProjeto(ProjetoExcluidoEvent evento) {
        int total = tarefaRepository.softDeleteByProjetoId(evento.projetoId(), LocalDateTime.now());
        log.info("Soft delete em {} tarefa(s) do projeto {} excluido", total, evento.projetoId());
    }
}
