package com.taskmanager.task.usecase;

import com.taskmanager.config.ConfiguracaoCache;
import com.taskmanager.project.MembroProjetoService;
import com.taskmanager.task.Tarefa;
import com.taskmanager.task.TarefaHelper;
import com.taskmanager.task.TarefaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Exclui uma tarefa do projeto. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExcluirTarefaUseCase {

    private final TarefaRepository tarefaRepository;
    private final MembroProjetoService membroProjetoService;
    private final TarefaHelper tarefaHelper;

    @Transactional
    @CacheEvict(value = ConfiguracaoCache.CACHE_RELATORIO_TAREFAS, key = "#projetoId")
    public void executar(Long projetoId, Long tarefaId, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        Tarefa tarefa = tarefaHelper.buscarEntidade(projetoId, tarefaId);
        tarefaRepository.delete(tarefa);
        log.info("Tarefa {} excluida por usuario {}", tarefaId, solicitanteId);
    }
}
