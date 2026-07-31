package com.taskmanager.task.usecase;

import com.taskmanager.project.MembroProjetoService;
import com.taskmanager.task.TarefaEventoBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Inscreve o solicitante nos eventos SSE de mudanca de status das tarefas de um projeto. */
@Component
@RequiredArgsConstructor
public class InscreverEventosTarefaUseCase {

    private final MembroProjetoService membroProjetoService;
    private final TarefaEventoBroadcaster tarefaEventoBroadcaster;

    public SseEmitter executar(Long projetoId, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        return tarefaEventoBroadcaster.inscrever(projetoId);
    }
}
