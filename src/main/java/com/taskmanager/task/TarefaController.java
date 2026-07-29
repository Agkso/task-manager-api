package com.taskmanager.task;

import com.taskmanager.common.dto.PaginaResposta;
import com.taskmanager.security.UsuarioAutenticado;
import com.taskmanager.task.dto.RequisicaoAtualizarStatus;
import com.taskmanager.task.dto.RequisicaoTarefa;
import com.taskmanager.task.dto.RespostaTarefa;
import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

@RestController
@RequestMapping("/api/projetos/{projetoId}/tarefas")
public class TarefaController {

    private final TarefaService tarefaService;

    public TarefaController(TarefaService tarefaService) {
        this.tarefaService = tarefaService;
    }

    @PostMapping
    public ResponseEntity<RespostaTarefa> criar(
            @PathVariable Long projetoId,
            @Valid @RequestBody RequisicaoTarefa requisicao,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        RespostaTarefa tarefa = tarefaService.criar(projetoId, requisicao, principal.getUsuarioId());
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
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        return tarefaService.listar(
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

    @GetMapping("/{tarefaId}")
    public RespostaTarefa buscar(
            @PathVariable Long projetoId,
            @PathVariable Long tarefaId,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        return tarefaService.buscarPorId(projetoId, tarefaId, principal.getUsuarioId());
    }

    @PutMapping("/{tarefaId}")
    public RespostaTarefa atualizar(
            @PathVariable Long projetoId,
            @PathVariable Long tarefaId,
            @Valid @RequestBody RequisicaoTarefa requisicao,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        return tarefaService.atualizar(projetoId, tarefaId, requisicao, principal.getUsuarioId());
    }

    @PatchMapping("/{tarefaId}/status")
    public RespostaTarefa mudarStatus(
            @PathVariable Long projetoId,
            @PathVariable Long tarefaId,
            @Valid @RequestBody RequisicaoAtualizarStatus requisicao,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        return tarefaService.mudarStatus(projetoId, tarefaId, requisicao, principal.getUsuarioId());
    }

    @DeleteMapping("/{tarefaId}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long projetoId,
            @PathVariable Long tarefaId,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        tarefaService.excluir(projetoId, tarefaId, principal.getUsuarioId());
        return ResponseEntity.noContent().build();
    }
}
