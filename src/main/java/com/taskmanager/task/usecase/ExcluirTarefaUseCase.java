package com.taskmanager.task.usecase;

import com.taskmanager.audit.AcaoAuditoria;
import com.taskmanager.audit.EventoAuditoria;
import com.taskmanager.audit.TipoEntidadeAuditoria;
import com.taskmanager.config.ConfiguracaoCache;
import com.taskmanager.project.MembroProjetoService;
import com.taskmanager.task.Tarefa;
import com.taskmanager.task.TarefaHelper;
import com.taskmanager.task.TarefaRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Exclui (soft delete - ver Tarefa.excluidoEm) uma tarefa do projeto. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExcluirTarefaUseCase {

    private final TarefaRepository tarefaRepository;
    private final MembroProjetoService membroProjetoService;
    private final TarefaHelper tarefaHelper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @CacheEvict(value = ConfiguracaoCache.CACHE_RELATORIO_TAREFAS, key = "#projetoId")
    public void executar(Long projetoId, Long tarefaId, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        Tarefa tarefa = tarefaHelper.buscarEntidade(projetoId, tarefaId);
        tarefa.setExcluidoEm(LocalDateTime.now());
        tarefaRepository.save(tarefa);
        log.info("Tarefa {} excluida por usuario {}", tarefaId, solicitanteId);
        eventPublisher.publishEvent(EventoAuditoria.de(
                AcaoAuditoria.TAREFA_EXCLUIDA, TipoEntidadeAuditoria.TAREFA, tarefaId, projetoId, solicitanteId));
    }
}
