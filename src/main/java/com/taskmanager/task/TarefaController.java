package com.taskmanager.task;

import com.taskmanager.common.dto.PaginaResposta;
import com.taskmanager.security.UsuarioAutenticado;
import com.taskmanager.task.dto.RequisicaoAtualizarStatus;
import com.taskmanager.task.dto.RequisicaoTarefa;
import com.taskmanager.task.dto.RespostaHistoricoTarefa;
import com.taskmanager.task.dto.RespostaRelatorio;
import com.taskmanager.task.dto.RespostaTarefa;
import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import com.taskmanager.task.usecase.AtualizarTarefaUseCase;
import com.taskmanager.task.usecase.BuscarHistoricoTarefaUseCase;
import com.taskmanager.task.usecase.BuscarTarefaUseCase;
import com.taskmanager.task.usecase.CriarTarefaUseCase;
import com.taskmanager.task.usecase.ExcluirTarefaUseCase;
import com.taskmanager.task.usecase.GerarRelatorioTarefaUseCase;
import com.taskmanager.task.usecase.ListarTarefasUseCase;
import com.taskmanager.task.usecase.MudarStatusTarefaUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/projetos/{projetoId}/tarefas")
@RequiredArgsConstructor
public class TarefaController {

    private final CriarTarefaUseCase criarTarefaUseCase;
    private final AtualizarTarefaUseCase atualizarTarefaUseCase;
    private final ExcluirTarefaUseCase excluirTarefaUseCase;
    private final MudarStatusTarefaUseCase mudarStatusTarefaUseCase;
    private final ListarTarefasUseCase listarTarefasUseCase;
    private final BuscarTarefaUseCase buscarTarefaUseCase;
    private final GerarRelatorioTarefaUseCase gerarRelatorioTarefaUseCase;
    private final BuscarHistoricoTarefaUseCase buscarHistoricoTarefaUseCase;

    @PostMapping
    public ResponseEntity<RespostaTarefa> criar(
            @PathVariable Long projetoId,
            @Valid @RequestBody RequisicaoTarefa requisicao,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        RespostaTarefa tarefa = criarTarefaUseCase.executar(projetoId, requisicao, principal.getUsuarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(tarefa);
    }

    @GetMapping
    public PaginaResposta<RespostaTarefa> listar(
            @PathVariable Long projetoId,
            @RequestParam(required = false) StatusTarefa status,
            @RequestParam(required = false) Prioridade prioridade,
            @RequestParam(required = false) Long responsavelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime prazoDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime prazoAte,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "criadoEm") String ordenarPor,
            @RequestParam(defaultValue = "asc") String direcao,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "pagina nao pode ser negativa") int pagina,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "tamanho deve ser no minimo 1")
                    @Max(value = 100, message = "tamanho deve ser no maximo 100")
                    int tamanho,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        return listarTarefasUseCase.executar(
                projetoId,
                principal.getUsuarioId(),
                status,
                prioridade,
                responsavelId,
                prazoDesde,
                prazoAte,
                busca,
                ordenarPor,
                direcao,
                pagina,
                tamanho);
    }

    @GetMapping("/relatorio")
    public RespostaRelatorio relatorio(
            @PathVariable Long projetoId, @AuthenticationPrincipal UsuarioAutenticado principal) {
        return gerarRelatorioTarefaUseCase.executar(projetoId, principal.getUsuarioId());
    }

    @GetMapping("/{tarefaId}")
    public RespostaTarefa buscar(
            @PathVariable Long projetoId,
            @PathVariable Long tarefaId,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        return buscarTarefaUseCase.executar(projetoId, tarefaId, principal.getUsuarioId());
    }

    @GetMapping("/{tarefaId}/historico")
    public List<RespostaHistoricoTarefa> historico(
            @PathVariable Long projetoId,
            @PathVariable Long tarefaId,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        return buscarHistoricoTarefaUseCase.executar(projetoId, tarefaId, principal.getUsuarioId());
    }

    @PutMapping("/{tarefaId}")
    public RespostaTarefa atualizar(
            @PathVariable Long projetoId,
            @PathVariable Long tarefaId,
            @Valid @RequestBody RequisicaoTarefa requisicao,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        return atualizarTarefaUseCase.executar(projetoId, tarefaId, requisicao, principal.getUsuarioId());
    }

    @PatchMapping("/{tarefaId}/status")
    public RespostaTarefa mudarStatus(
            @PathVariable Long projetoId,
            @PathVariable Long tarefaId,
            @Valid @RequestBody RequisicaoAtualizarStatus requisicao,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        return mudarStatusTarefaUseCase.executar(projetoId, tarefaId, requisicao, principal.getUsuarioId());
    }

    @DeleteMapping("/{tarefaId}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long projetoId,
            @PathVariable Long tarefaId,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        excluirTarefaUseCase.executar(projetoId, tarefaId, principal.getUsuarioId());
        return ResponseEntity.noContent().build();
    }
}
