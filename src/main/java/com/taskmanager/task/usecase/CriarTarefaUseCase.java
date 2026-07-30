package com.taskmanager.task.usecase;

import com.taskmanager.config.ConfiguracaoCache;
import com.taskmanager.project.MembroProjetoService;
import com.taskmanager.project.Projeto;
import com.taskmanager.project.ProjetoService;
import com.taskmanager.task.Tarefa;
import com.taskmanager.task.TarefaHelper;
import com.taskmanager.task.TarefaMapper;
import com.taskmanager.task.TarefaRepository;
import com.taskmanager.task.dto.RequisicaoTarefa;
import com.taskmanager.task.dto.RespostaTarefa;
import com.taskmanager.task.enums.StatusTarefa;
import com.taskmanager.user.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Cria uma tarefa TODO num projeto, validando que o solicitante e membro e que o responsavel (se houver) tambem e. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CriarTarefaUseCase {

    private final TarefaRepository tarefaRepository;
    private final MembroProjetoService membroProjetoService;
    private final ProjetoService projetoService;
    private final TarefaHelper tarefaHelper;
    private final TarefaMapper tarefaMapper;

    @Transactional
    @CacheEvict(value = ConfiguracaoCache.CACHE_RELATORIO_TAREFAS, key = "#projetoId")
    public RespostaTarefa executar(Long projetoId, RequisicaoTarefa requisicao, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        Projeto projeto = projetoService.buscarPorId(projetoId);
        Usuario responsavel = tarefaHelper.resolverResponsavel(projetoId, requisicao.responsavelId());

        Tarefa tarefa = Tarefa.builder()
                .projeto(projeto)
                .titulo(requisicao.titulo())
                .descricao(requisicao.descricao())
                .prioridade(requisicao.prioridade())
                .prazo(requisicao.prazo())
                .responsavel(responsavel)
                .status(StatusTarefa.TODO)
                .build();

        Tarefa salva = tarefaRepository.save(tarefa);
        log.info("Tarefa {} criada no projeto {} por usuario {}", salva.getId(), projetoId, solicitanteId);
        return tarefaMapper.paraResposta(salva);
    }
}
