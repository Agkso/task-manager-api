package com.taskmanager.task.usecase;

import com.taskmanager.config.ConfiguracaoCache;
import com.taskmanager.project.MembroProjeto;
import com.taskmanager.project.MembroProjetoService;
import com.taskmanager.task.RegrasTransicaoStatusTarefa;
import com.taskmanager.task.Tarefa;
import com.taskmanager.task.TarefaHelper;
import com.taskmanager.task.TarefaMapper;
import com.taskmanager.task.TarefaRepository;
import com.taskmanager.task.TarefaStatusAlteradoEvent;
import com.taskmanager.task.dto.RequisicaoAtualizarStatus;
import com.taskmanager.task.dto.RespostaTarefa;
import com.taskmanager.task.enums.StatusTarefa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Muda o status de uma tarefa, delegando as 3 regras de transicao para
 * {@link RegrasTransicaoStatusTarefa} e publicando {@link TarefaStatusAlteradoEvent}
 * para quem grava o historico (ver {@code HistoricoTarefaListener}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MudarStatusTarefaUseCase {

    private final TarefaRepository tarefaRepository;
    private final MembroProjetoService membroProjetoService;
    private final TarefaHelper tarefaHelper;
    private final TarefaMapper tarefaMapper;
    private final RegrasTransicaoStatusTarefa regrasTransicaoStatusTarefa;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @CacheEvict(value = ConfiguracaoCache.CACHE_RELATORIO_TAREFAS, key = "#projetoId")
    public RespostaTarefa executar(
            Long projetoId, Long tarefaId, RequisicaoAtualizarStatus requisicao, Long solicitanteId) {
        MembroProjeto membro = membroProjetoService.obterMembro(projetoId, solicitanteId);
        Tarefa tarefa = tarefaHelper.buscarEntidade(projetoId, tarefaId);

        long tarefasEmAndamento = tarefa.getResponsavel() == null
                ? 0
                : tarefaRepository.countByResponsavelIdAndStatusAndExcluidoEmIsNull(
                        tarefa.getResponsavel().getId(), StatusTarefa.IN_PROGRESS);

        StatusTarefa statusAnterior = tarefa.getStatus();
        regrasTransicaoStatusTarefa.validar(tarefa, requisicao.status(), membro.getPapel(), tarefasEmAndamento);

        tarefa.setStatus(requisicao.status());
        RespostaTarefa resposta = tarefaMapper.paraResposta(tarefaRepository.save(tarefa));
        eventPublisher.publishEvent(
                new TarefaStatusAlteradoEvent(tarefaId, projetoId, solicitanteId, statusAnterior, requisicao.status()));
        log.info(
                "Tarefa {} mudou de status {} para {} (solicitado por usuario {})",
                tarefaId,
                statusAnterior,
                requisicao.status(),
                solicitanteId);
        return resposta;
    }
}
