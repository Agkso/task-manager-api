package com.taskmanager.task.usecase;

import com.taskmanager.common.dto.PaginaResposta;
import com.taskmanager.project.MembroProjetoService;
import com.taskmanager.task.Tarefa;
import com.taskmanager.task.TarefaMapper;
import com.taskmanager.task.TarefaRepository;
import com.taskmanager.task.TarefaSpecifications;
import com.taskmanager.task.dto.RespostaTarefa;
import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Lista as tarefas de um projeto com filtros, ordenacao e paginacao. */
@Component
@RequiredArgsConstructor
public class ListarTarefasUseCase {

    private static final int TAMANHO_PAGINA_PADRAO = 20;
    private static final int TAMANHO_PAGINA_MAXIMO = 100;

    private final TarefaRepository tarefaRepository;
    private final MembroProjetoService membroProjetoService;
    private final TarefaMapper tarefaMapper;

    @Transactional(readOnly = true)
    public PaginaResposta<RespostaTarefa> executar(
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
                paginaTarefas.getContent().stream().map(tarefaMapper::paraResposta).toList();

        return new PaginaResposta<>(
                conteudo, paginaTarefas.getNumber(), paginaTarefas.getTotalPages(), paginaTarefas.getTotalElements());
    }

    private Sort construirOrdenacaoSimples(String ordenarPor, boolean descendente) {
        Sort.Direction sentido = descendente ? Sort.Direction.DESC : Sort.Direction.ASC;
        String propriedade = "prazo".equals(ordenarPor) ? "prazo" : "criadoEm";
        return Sort.by(sentido, propriedade);
    }
}
