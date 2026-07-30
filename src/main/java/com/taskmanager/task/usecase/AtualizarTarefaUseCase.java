package com.taskmanager.task.usecase;

import com.taskmanager.config.ConfiguracaoCache;
import com.taskmanager.project.MembroProjetoService;
import com.taskmanager.task.Tarefa;
import com.taskmanager.task.TarefaHelper;
import com.taskmanager.task.TarefaMapper;
import com.taskmanager.task.TarefaRepository;
import com.taskmanager.task.dto.RequisicaoTarefa;
import com.taskmanager.task.dto.RespostaTarefa;
import com.taskmanager.user.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Atualiza os campos editaveis de uma tarefa (titulo, descricao, prioridade, prazo e responsavel). */
@Slf4j
@Component
@RequiredArgsConstructor
public class AtualizarTarefaUseCase {

    private final TarefaRepository tarefaRepository;
    private final MembroProjetoService membroProjetoService;
    private final TarefaHelper tarefaHelper;
    private final TarefaMapper tarefaMapper;

    @Transactional
    @CacheEvict(value = ConfiguracaoCache.CACHE_RELATORIO_TAREFAS, key = "#projetoId")
    public RespostaTarefa executar(Long projetoId, Long tarefaId, RequisicaoTarefa requisicao, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        Tarefa tarefa = tarefaHelper.buscarEntidade(projetoId, tarefaId);
        Usuario responsavel = tarefaHelper.resolverResponsavel(projetoId, requisicao.responsavelId());

        tarefa.setTitulo(requisicao.titulo());
        tarefa.setDescricao(requisicao.descricao());
        tarefa.setPrioridade(requisicao.prioridade());
        tarefa.setPrazo(requisicao.prazo());
        tarefa.setResponsavel(responsavel);

        RespostaTarefa resposta = tarefaMapper.paraResposta(tarefaRepository.save(tarefa));
        log.info("Tarefa {} atualizada por usuario {}", tarefaId, solicitanteId);
        return resposta;
    }
}
