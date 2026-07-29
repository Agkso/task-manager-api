package com.taskmanager.task;

import com.taskmanager.common.dto.PaginaResposta;
import com.taskmanager.exception.RecursoNaoEncontradoException;
import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.project.MembroProjeto;
import com.taskmanager.project.MembroProjetoRepository;
import com.taskmanager.project.Projeto;
import com.taskmanager.project.ProjetoService;
import com.taskmanager.project.enums.Papel;
import com.taskmanager.task.dto.RequisicaoAtualizarStatus;
import com.taskmanager.task.dto.RequisicaoTarefa;
import com.taskmanager.task.dto.RespostaRelatorio;
import com.taskmanager.task.dto.RespostaTarefa;
import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarefaService {

    private static final int WIP_LIMITE_IN_PROGRESS = 5;

    private final TarefaRepository tarefaRepository;
    private final ProjetoService projetoService;
    private final MembroProjetoRepository membroProjetoRepository;
    private final UsuarioRepository usuarioRepository;

    public TarefaService(
            TarefaRepository tarefaRepository,
            ProjetoService projetoService,
            MembroProjetoRepository membroProjetoRepository,
            UsuarioRepository usuarioRepository) {
        this.tarefaRepository = tarefaRepository;
        this.projetoService = projetoService;
        this.membroProjetoRepository = membroProjetoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public RespostaTarefa criar(Long projetoId, RequisicaoTarefa requisicao, Long solicitanteId) {
        projetoService.obterMembro(projetoId, solicitanteId);
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
        projetoService.obterMembro(projetoId, solicitanteId);
        return RespostaTarefa.de(buscarEntidade(projetoId, tarefaId));
    }

    @Transactional
    public RespostaTarefa atualizar(
            Long projetoId, Long tarefaId, RequisicaoTarefa requisicao, Long solicitanteId) {
        projetoService.obterMembro(projetoId, solicitanteId);
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
        projetoService.obterMembro(projetoId, solicitanteId);
        Tarefa tarefa = buscarEntidade(projetoId, tarefaId);
        tarefaRepository.delete(tarefa);
    }

    @Transactional
    public RespostaTarefa mudarStatus(
            Long projetoId, Long tarefaId, RequisicaoAtualizarStatus requisicao, Long solicitanteId) {
        MembroProjeto membro = projetoService.obterMembro(projetoId, solicitanteId);
        Tarefa tarefa = buscarEntidade(projetoId, tarefaId);

        StatusTarefa statusAtual = tarefa.getStatus();
        StatusTarefa novoStatus = requisicao.status();

        if (statusAtual == StatusTarefa.DONE && novoStatus == StatusTarefa.TODO) {
            throw new RegraNegocioException(
                    "Uma tarefa concluida (DONE) nao pode voltar para TODO, apenas para IN_PROGRESS");
        }

        if (novoStatus == StatusTarefa.DONE
                && tarefa.getPrioridade() == Prioridade.CRITICAL
                && membro.getPapel() != Papel.ADMIN) {
            throw new AccessDeniedException(
                    "Apenas o ADMIN do projeto pode concluir uma tarefa de prioridade CRITICAL");
        }

        if (novoStatus == StatusTarefa.IN_PROGRESS && statusAtual != StatusTarefa.IN_PROGRESS) {
            Usuario responsavel = tarefa.getResponsavel();
            if (responsavel != null) {
                long emAndamento =
                        tarefaRepository.countByResponsavelIdAndStatus(responsavel.getId(), StatusTarefa.IN_PROGRESS);
                if (emAndamento >= WIP_LIMITE_IN_PROGRESS) {
                    throw new RegraNegocioException("Limite de "
                            + WIP_LIMITE_IN_PROGRESS
                            + " tarefas em andamento (IN_PROGRESS) atingido para este responsavel");
                }
            }
        }

        tarefa.setStatus(novoStatus);
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
        projetoService.obterMembro(projetoId, solicitanteId);

        Specification<Tarefa> spec = Specification.where(TarefaSpecifications.doProjeto(projetoId))
                .and(TarefaSpecifications.comStatus(status))
                .and(TarefaSpecifications.comPrioridade(prioridade))
                .and(TarefaSpecifications.comResponsavel(responsavelId))
                .and(TarefaSpecifications.comPrazoDesde(prazoDesde))
                .and(TarefaSpecifications.comPrazoAte(prazoAte))
                .and(TarefaSpecifications.comTextoEm(texto));

        List<Tarefa> tarefas = tarefaRepository.findAll(spec);
        tarefas.sort(comparadorDe(ordenarPor, direcao));

        int paginaSegura = Math.max(pagina, 0);
        int tamanhoSeguro = tamanho <= 0 ? 20 : tamanho;
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
        projetoService.obterMembro(projetoId, solicitanteId);

        Map<StatusTarefa, Long> porStatus = new EnumMap<>(StatusTarefa.class);
        for (StatusTarefa status : StatusTarefa.values()) {
            porStatus.put(status, 0L);
        }
        tarefaRepository.contarPorStatus(projetoId).forEach(c -> porStatus.put(c.getStatus(), c.getTotal()));

        Map<Prioridade, Long> porPrioridade = new EnumMap<>(Prioridade.class);
        for (Prioridade prioridade : Prioridade.values()) {
            porPrioridade.put(prioridade, 0L);
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

    /**
     * Ordenacao por prioridade usa o ordinal do enum (LOW=0 ... CRITICAL=3),
     * que so funciona porque Prioridade foi declarado nessa ordem crescente
     * de severidade - se a ordem de declaracao mudar, isso quebra.
     */
    private Comparator<Tarefa> comparadorDe(String ordenarPor, String direcao) {
        Comparator<Tarefa> comparador =
                switch (ordenarPor == null ? "" : ordenarPor) {
                    case "prioridade" -> Comparator.comparingInt(t -> t.getPrioridade().ordinal());
                    case "prazo" -> Comparator.comparing(
                            Tarefa::getPrazo, Comparator.nullsLast(Comparator.naturalOrder()));
                    default -> Comparator.comparing(Tarefa::getCriadoEm);
                };

        return "desc".equalsIgnoreCase(direcao) ? comparador.reversed() : comparador;
    }
}
