package com.taskmanager.project;

import com.taskmanager.project.dto.RequisicaoAdicionarMembro;
import com.taskmanager.project.dto.RequisicaoProjeto;
import com.taskmanager.project.dto.RespostaMembro;
import com.taskmanager.project.dto.RespostaProjeto;
import com.taskmanager.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
public class ProjetoController {

    private final ProjetoService projetoService;
    private final MembroProjetoService membroProjetoService;
    private final ProjetoMapper projetoMapper;
    private final MembroMapper membroMapper;

    @PostMapping
    public ResponseEntity<RespostaProjeto> criar(
            @Valid @RequestBody RequisicaoProjeto requisicao, @AuthenticationPrincipal UsuarioAutenticado principal) {
        Projeto projeto = projetoService.criar(requisicao, principal.getUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(projetoMapper.paraResposta(projeto));
    }

    @GetMapping
    public List<RespostaProjeto> listar(@AuthenticationPrincipal UsuarioAutenticado principal) {
        return projetoService.listarDoUsuario(principal.getUsuarioId()).stream()
                .map(projetoMapper::paraResposta)
                .toList();
    }

    @GetMapping("/{id}")
    public RespostaProjeto buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal) {
        membroProjetoService.obterMembro(id, principal.getUsuarioId());
        return projetoMapper.paraResposta(projetoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public RespostaProjeto atualizar(
            @PathVariable Long id,
            @Valid @RequestBody RequisicaoProjeto requisicao,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        return projetoMapper.paraResposta(projetoService.atualizar(id, requisicao, principal.getUsuarioId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal) {
        projetoService.excluir(id, principal.getUsuarioId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/membros")
    public List<RespostaMembro> listarMembros(
            @PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal) {
        return membroProjetoService.listar(id, principal.getUsuarioId()).stream()
                .map(membroMapper::paraResposta)
                .toList();
    }

    @PostMapping("/{id}/membros")
    public ResponseEntity<RespostaMembro> adicionarMembro(
            @PathVariable Long id,
            @Valid @RequestBody RequisicaoAdicionarMembro requisicao,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        var membro = membroProjetoService.adicionar(id, requisicao, principal.getUsuarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(membroMapper.paraResposta(membro));
    }

    @DeleteMapping("/{id}/membros/{usuarioId}")
    public ResponseEntity<Void> removerMembro(
            @PathVariable Long id,
            @PathVariable Long usuarioId,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        membroProjetoService.remover(id, usuarioId, principal.getUsuarioId());
        return ResponseEntity.noContent().build();
    }
}
