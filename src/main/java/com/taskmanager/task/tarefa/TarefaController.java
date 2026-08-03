package com.taskmanager.task.tarefa;

import com.taskmanager.common.dto.PaginaResposta;
import com.taskmanager.security.UsuarioAutenticado;
import com.taskmanager.task.dto.RequisicaoAtualizarStatus;
import com.taskmanager.task.dto.RequisicaoFiltroTarefa;
import com.taskmanager.task.dto.RequisicaoTarefa;
import com.taskmanager.task.dto.RespostaHistoricoTarefa;
import com.taskmanager.task.dto.RespostaRelatorio;
import com.taskmanager.task.dto.RespostaTarefa;
import com.taskmanager.task.usecase.AtualizarTarefaUseCase;
import com.taskmanager.task.usecase.BuscarHistoricoTarefaUseCase;
import com.taskmanager.task.usecase.BuscarTarefaUseCase;
import com.taskmanager.task.usecase.CriarTarefaUseCase;
import com.taskmanager.task.usecase.ExcluirTarefaUseCase;
import com.taskmanager.task.usecase.GerarRelatorioTarefaUseCase;
import com.taskmanager.task.usecase.InscreverEventosTarefaUseCase;
import com.taskmanager.task.usecase.ListarTarefasUseCase;
import com.taskmanager.task.usecase.MudarStatusTarefaUseCase;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final InscreverEventosTarefaUseCase inscreverEventosTarefaUseCase;

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
            @Valid @ModelAttribute RequisicaoFiltroTarefa filtro,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        return listarTarefasUseCase.executar(projetoId, principal.getUsuarioId(), filtro);
    }

    /**
     * Stream SSE (Server-Sent Events) de mudancas de status das tarefas
     * deste projeto - pensado pro board do frontend atualizar sozinho
     * quando outro usuario move um card, sem polling. Autenticacao aqui
     * aceita token via query param (?token=), alem do header Bearer normal -
     * ver o javadoc em FiltroAutenticacaoJwt.extrairToken pro motivo
     * (EventSource do browser nao manda headers customizados).
     */
    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter eventos(
            @PathVariable Long projetoId, @AuthenticationPrincipal UsuarioAutenticado principal) {
        return inscreverEventosTarefaUseCase.executar(projetoId, principal.getUsuarioId());
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
