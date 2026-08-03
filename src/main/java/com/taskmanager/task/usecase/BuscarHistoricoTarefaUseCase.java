package com.taskmanager.task.usecase;

import com.taskmanager.project.membro.MembroProjetoService;
import com.taskmanager.task.historico.HistoricoTarefaMapper;
import com.taskmanager.task.historico.HistoricoTarefaRepository;
import com.taskmanager.task.tarefa.TarefaHelper;
import com.taskmanager.task.dto.RespostaHistoricoTarefa;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Lista o historico de mudancas de status de uma tarefa, da mais recente para a mais antiga. */
@Component
@RequiredArgsConstructor
public class BuscarHistoricoTarefaUseCase {

    private final MembroProjetoService membroProjetoService;
    private final TarefaHelper tarefaHelper;
    private final HistoricoTarefaRepository historicoTarefaRepository;
    private final HistoricoTarefaMapper historicoTarefaMapper;

    @Transactional(readOnly = true)
    public List<RespostaHistoricoTarefa> executar(Long projetoId, Long tarefaId, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        tarefaHelper.buscarEntidade(projetoId, tarefaId);
        return historicoTarefaRepository.findByTarefaIdOrderByAlteradoEmDesc(tarefaId).stream()
                .map(historicoTarefaMapper::paraResposta)
                .toList();
    }
}
