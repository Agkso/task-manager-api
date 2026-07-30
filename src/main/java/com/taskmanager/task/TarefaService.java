package com.taskmanager.task;

import com.taskmanager.common.dto.PaginaResposta;
import com.taskmanager.exception.RecursoNaoEncontradoException;
import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.project.MembroProjeto;
import com.taskmanager.project.MembroProjetoService;
import com.taskmanager.project.Projeto;
import com.taskmanager.project.ProjetoService;
import com.taskmanager.task.dto.RequisicaoAtualizarStatus;
import com.taskmanager.task.dto.RequisicaoTarefa;
import com.taskmanager.task.dto.RespostaHistoricoTarefa;
import com.taskmanager.task.dto.RespostaRelatorio;
import com.taskmanager.task.dto.RespostaTarefa;
import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TarefaService {

    private static final int TAMANHO_PAGINA_PADRAO = 20;
    private static final int TAMANHO_PAGINA_MAXIMO = 100;

    private final TarefaRepository tarefaRepository;
    private final HistoricoTarefaRepository historicoTarefaRepository;
    private final MembroProjetoService membroProjetoService;
    private final ProjetoService projetoService;
    private final UsuarioRepository usuarioRepository;
    private final RegrasTransicaoStatusTarefa regrasTransicaoStatusTarefa;
    private final ApplicationEventPublisher eventPublisher;

    public TarefaService(
            TarefaRepository tarefaRepository,
            HistoricoTarefaRepository historicoTarefaRepository,
            MembroProjetoService membroProjetoService,
            ProjetoService projetoService,
            UsuarioRepository usuarioRepository,
            RegrasTransicaoStatusTarefa regrasTransicaoStatusTarefa,
            ApplicationEventPublisher eventPublisher) {
        this.tarefaRepository = tarefaRepository;
        this.historicoTarefaRepository = historicoTarefaRepository;
        this.membroProjetoService = membroProjetoService;
        this.projetoService = projetoService;
        this.usuarioRepository = usuarioRepository;
        this.regrasTransicaoStatusTarefa = regrasTransicaoStatusTarefa;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RespostaTarefa criar(Long projetoId, RequisicaoTarefa requisicao, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        Projeto projeto = projetoService.buscarPorId(projetoId);
        Usuario responsavel = resolverResponsavel(projetoId, requisicao.responsavelId());

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
        return RespostaTarefa.de(salva);
    }

    @Transactional(readOnly = true)
    public RespostaTarefa buscarPorId(Long projetoId, Long tarefaId, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        return RespostaTarefa.de(buscarEntidade(projetoId, tarefaId));
    }

    @Transactional
    public RespostaTarefa atualizar(
            Long projetoId, Long tarefaId, RequisicaoTarefa requisicao, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        Tarefa tarefa = buscarEntidade(projetoId, tarefaId);
        Usuario responsavel = resolverResponsavel(projetoId, requisicao.responsavelId());

        tarefa.setTitulo(requisicao.titulo());
        tarefa.setDescricao(requisicao.descricao());
        tarefa.setPrioridade(requisicao.prioridade());
        tarefa.setPrazo(requisicao.prazo());
        tarefa.setResponsavel(responsavel);

        RespostaTarefa resposta = RespostaTarefa.de(tarefaRepository.save(tarefa));
        log.info("Tarefa {} atualizada por usuario {}", tarefaId, solicitanteId);
        return resposta;
    }

    @Transactional
    public void excluir(Long projetoId, Long tarefaId, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        Tarefa tarefa = buscarEntidade(projetoId, tarefaId);
        tarefaRepository.delete(tarefa);
        log.info("Tarefa {} excluida por usuario {}", tarefaId, solicitanteId);
    }

    @Transactional
    public RespostaTarefa mudarStatus(
            Long projetoId, Long tarefaId, RequisicaoAtualizarStatus requisicao, Long solicitanteId) {
        MembroProjeto membro = membroProjetoService.obterMembro(projetoId, solicitanteId);
        Tarefa tarefa = buscarEntidade(projetoId, tarefaId);

        long tarefasEmAndamento = tarefa.getResponsavel() == null
                ? 0
                : tarefaRepository.countByResponsavelIdAndStatus(
                        tarefa.getResponsavel().getId(), StatusTarefa.IN_PROGRESS);

        StatusTarefa statusAnterior = tarefa.getStatus();
        regrasTransicaoStatusTarefa.validar(tarefa, requisicao.status(), membro.getPapel(), tarefasEmAndamento);

        tarefa.setStatus(requisicao.status());
        RespostaTarefa resposta = RespostaTarefa.de(tarefaRepository.save(tarefa));
        eventPublisher.publishEvent(
                new TarefaStatusAlteradoEvent(tarefaId, solicitanteId, statusAnterior, requisicao.status()));
        log.info(
                "Tarefa {} mudou de status {} para {} (solicitado por usuario {})",
                tarefaId,
                statusAnterior,
                requisicao.status(),
                solicitanteId);
        return resposta;
    }

    @Transactional(readOnly = true)
    public PaginaResposta<RespostaTarefa> listar(
            Long projetoId,
            Long solicitanteId,
            StatusTarefa status,
            Prioridade prioridade,
            Long responsavelId,
            LocalDateTime prazoDesde,
            LocalDateTime prazoAte,
            String texto,
            String ordenarPor,
            String direcao,
            int pagina,
            int tamanho) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);

        boolean ordenarPorPrioridade = "prioridade".equals(ordenarPor);
        boolean descendente = "desc".equalsIgnoreCase(direcao);

        Specification<Tarefa> spec = Specification.where(TarefaSpecifications.doProjeto(projetoId))
                .and(TarefaSpecifications.comStatus(status))
                .and(TarefaSpecifications.comPrioridade(prioridade))
                .and(TarefaSpecifications.comResponsavel(responsavelId))
                .and(TarefaSpecifications.comPrazoDesde(prazoDesde))
                .and(TarefaSpecifications.comPrazoAte(prazoAte))
                .and(TarefaSpecifications.comTextoEm(texto))
                .and(TarefaSpecifications.comResponsavelCarregado());

        if (ordenarPorPrioridade) {
            spec = spec.and(TarefaSpecifications.ordenarPorPrioridade(descendente));
        }

        int paginaSegura = Math.max(pagina, 0);
        int tamanhoSeguro = tamanho <= 0 ? TAMANHO_PAGINA_PADRAO : Math.min(tamanho, TAMANHO_PAGINA_MAXIMO);
        // prioridade: a ordenacao ja foi fixada na Specification via query.orderBy(...);
        // Sort.unsorted() aqui evita que o Pageable sobrescreva isso.
        Sort sort = ordenarPorPrioridade ? Sort.unsorted() : construirOrdenacaoSimples(ordenarPor, descendente);
        Pageable pageable = PageRequest.of(paginaSegura, tamanhoSeguro, sort);

        Page<Tarefa> paginaTarefas = tarefaRepository.findAll(spec, pageable);
        List<RespostaTarefa> conteudo =
                paginaTarefas.getContent().stream().map(RespostaTarefa::de).toList();

        return new PaginaResposta<>(
                conteudo, paginaTarefas.getNumber(), paginaTarefas.getTotalPages(), paginaTarefas.getTotalElements());
    }

    private Sort construirOrdenacaoSimples(String ordenarPor, boolean descendente) {
        Sort.Direction sentido = descendente ? Sort.Direction.DESC : Sort.Direction.ASC;
        String propriedade = "prazo".equals(ordenarPor) ? "prazo" : "criadoEm";
        return Sort.by(sentido, propriedade);
    }

    @Transactional(readOnly = true)
    public RespostaRelatorio gerarRelatorio(Long projetoId, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);

        Map<StatusTarefa, Long> porStatus = new EnumMap<>(StatusTarefa.class);
        for (StatusTarefa statusTarefa : StatusTarefa.values()) {
            porStatus.put(statusTarefa, 0L);
        }
        tarefaRepository.contarPorStatus(projetoId).forEach(c -> porStatus.put(c.getStatus(), c.getTotal()));

        Map<Prioridade, Long> porPrioridade = new EnumMap<>(Prioridade.class);
        for (Prioridade prioridadeValor : Prioridade.values()) {
            porPrioridade.put(prioridadeValor, 0L);
        }
        tarefaRepository
                .contarPorPrioridade(projetoId)
                .forEach(c -> porPrioridade.put(c.getPrioridade(), c.getTotal()));

        return new RespostaRelatorio(porStatus, porPrioridade);
    }

    @Transactional(readOnly = true)
    public List<RespostaHistoricoTarefa> buscarHistorico(Long projetoId, Long tarefaId, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        buscarEntidade(projetoId, tarefaId);
        return historicoTarefaRepository.findByTarefaIdOrderByAlteradoEmDesc(tarefaId).stream()
                .map(RespostaHistoricoTarefa::de)
                .toList();
    }

    private Tarefa buscarEntidade(Long projetoId, Long tarefaId) {
        Tarefa tarefa = tarefaRepository
                .findById(tarefaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa nao encontrada: " + tarefaId));
        if (!tarefa.getProjeto().getId().equals(projetoId)) {
            throw new RecursoNaoEncontradoException("Tarefa nao encontrada: " + tarefaId);
        }
        return tarefa;
    }

    private Usuario resolverResponsavel(Long projetoId, Long responsavelId) {
        if (responsavelId == null) {
            return null;
        }
        if (!membroProjetoService.ehMembro(projetoId, responsavelId)) {
            throw new RegraNegocioException("O responsavel informado nao e membro deste projeto");
        }
        return usuarioRepository
                .findById(responsavelId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + responsavelId));
    }
}
