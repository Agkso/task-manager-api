package com.taskmanager.task.usecase;

import com.taskmanager.project.MembroProjetoService;
import com.taskmanager.task.TarefaHelper;
import com.taskmanager.task.TarefaMapper;
import com.taskmanager.task.dto.RespostaTarefa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Busca uma tarefa especifica de um projeto pelo id. */
@Component
@RequiredArgsConstructor
public class BuscarTarefaUseCase {

    private final MembroProjetoService membroProjetoService;
    private final TarefaHelper tarefaHelper;
    private final TarefaMapper tarefaMapper;

    @Transactional(readOnly = true)
    public RespostaTarefa executar(Long projetoId, Long tarefaId, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        return tarefaMapper.paraResposta(tarefaHelper.buscarEntidade(projetoId, tarefaId));
    }
}
