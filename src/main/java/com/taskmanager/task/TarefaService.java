package com.taskmanager.task;

import com.taskmanager.common.dto.PaginaResposta;
import com.taskmanager.exception.RecursoNaoEncontradoException;
import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.project.MembroProjeto;
import com.taskmanager.project.MembroProjetoRepository;
import com.taskmanager.project.MembroProjetoService;
import com.taskmanager.project.Projeto;
import com.taskmanager.project.ProjetoService;
import com.taskmanager.task.dto.RequisicaoAtualizarStatus;
import com.taskmanager.task.dto.RequisicaoTarefa;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarefaService {

    private static final int TAMANHO_PAGINA_PADRAO = 20;
    private static final int TAMANHO_PAGINA_MAXIMO = 100;

    private final TarefaRepository tarefaRepository;
    private final MembroProjetoService membroProjetoService;
    private final ProjetoService projetoService;
    private final MembroProjetoRepository membroProjetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RegrasTransicaoStatusTarefa regrasTransicaoStatusTarefa;

    public TarefaService(
            TarefaRepository tarefaRepository,
            MembroProjetoService membroProjetoService,
            ProjetoService projetoService,
            MembroProjetoRepository membroProjetoRepository,
            UsuarioRepository usuarioRepository,
            RegrasTransicaoStatusTarefa regrasTransicaoStatusTarefa) {
        this.tarefaRepository = tarefaRepository;
        this.membroProjetoService = membroProjetoService;
        this.projetoService = projetoService;
        this.membroProjetoRepository = membroProjetoRepository;
        this.usuarioRepository = usuarioRepository;
        this.regrasTransicaoStatusTarefa = regrasTransicaoStatusTarefa;
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

        return RespostaTarefa.de(tarefaRepository.save(tarefa));
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

        return RespostaTarefa.de(tarefaRepository.save(tarefa));
    }

    @Transactional
    public void excluir(Long projetoId, Long tarefaId, Long solicitanteId) {
        membroProjetoService.obterMembro(projetoId, solicitanteId);
        Tarefa tarefa = buscarEntidade(projetoId, tarefaId);
        tarefaRepository.delete(tarefa);
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

        regrasTransicaoStatusTarefa.validar(tarefa, requisicao.status(), membro.getPapel(), tarefasEmAndamento);

        tarefa.setStatus(requisicao.status());
        return RespostaTarefa.de(tarefaRepository.save(tarefa));
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

        Specification<Tarefa> spec = Specification.where(TarefaSpecifications.doProjeto(projetoId))
                .and(TarefaSpecifications.comStatus(status))
                .and(TarefaSpecifications.comPrioridade(prioridade))
                .and(TarefaSpecifications.comResponsavel(responsavelId))
                .and(TarefaSpecifications.comPrazoDesde(prazoDesde))
                .and(TarefaSpecifications.comPrazoAte(prazoAte))
                .and(TarefaSpecifications.comTextoEm(texto))
                .and(TarefaSpecifications.comResponsavelCarregado());

        List<Tarefa> tarefas = tarefaRepository.findAll(spec);
        tarefas.sort(TarefaOrdenador.comparador(ordenarPor, direcao));

        int paginaSegura = Math.max(pagina, 0);
        int tamanhoSeguro = tamanho <= 0 ? TAMANHO_PAGINA_PADRAO : Math.min(tamanho, TAMANHO_PAGINA_MAXIMO);
        int totalElementos = tarefas.size();
        int totalPaginas = (int) Math.ceil(totalElementos / (double) tamanhoSeguro);
        int inicio = Math.min(paginaSegura * tamanhoSeguro, totalElementos);
        int fim = Math.min(inicio + tamanhoSeguro, totalElementos);

        List<RespostaTarefa> conteudo =
                tarefas.subList(inicio, fim).stream().map(RespostaTarefa::de).toList();

        return new PaginaResposta<>(conteudo, paginaSegura, totalPaginas, totalElementos);
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
        if (!membroProjetoRepository.existsByProjetoIdAndUsuarioId(projetoId, responsavelId)) {
            throw new RegraNegocioException("O responsavel informado nao e membro deste projeto");
        }
        return usuarioRepository
                .findById(responsavelId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado: " + responsavelId));
    }
}
