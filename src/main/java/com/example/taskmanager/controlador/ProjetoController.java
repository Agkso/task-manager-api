package com.example.taskmanager.controlador;

import com.example.taskmanager.dominio.Projeto;
import com.example.taskmanager.dto.projeto.RequisicaoAdicionarMembro;
import com.example.taskmanager.dto.projeto.RequisicaoProjeto;
import com.example.taskmanager.dto.projeto.RespostaMembro;
import com.example.taskmanager.dto.projeto.RespostaProjeto;
import com.example.taskmanager.seguranca.UsuarioAutenticado;
import com.example.taskmanager.servico.ProjetoService;
import jakarta.validation.Valid;
import java.util.List;
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
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    @PostMapping
    public ResponseEntity<RespostaProjeto> criar(
            @Valid @RequestBody RequisicaoProjeto requisicao, @AuthenticationPrincipal UsuarioAutenticado principal) {
        Projeto projeto = projetoService.criar(requisicao, principal.getUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(RespostaProjeto.de(projeto));
    }

    @GetMapping
    public List<RespostaProjeto> listar(@AuthenticationPrincipal UsuarioAutenticado principal) {
        return projetoService.listarDoUsuario(principal.getUsuarioId()).stream()
                .map(RespostaProjeto::de)
                .toList();
    }

    @GetMapping("/{id}")
    public RespostaProjeto buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal) {
        projetoService.obterMembro(id, principal.getUsuarioId());
        return RespostaProjeto.de(projetoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public RespostaProjeto atualizar(
            @PathVariable Long id,
            @Valid @RequestBody RequisicaoProjeto requisicao,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        return RespostaProjeto.de(projetoService.atualizar(id, requisicao, principal.getUsuarioId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal) {
        projetoService.excluir(id, principal.getUsuarioId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/membros")
    public List<RespostaMembro> listarMembros(
            @PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado principal) {
        return projetoService.listarMembros(id, principal.getUsuarioId()).stream()
                .map(RespostaMembro::de)
                .toList();
    }

    @PostMapping("/{id}/membros")
    public ResponseEntity<RespostaMembro> adicionarMembro(
            @PathVariable Long id,
            @Valid @RequestBody RequisicaoAdicionarMembro requisicao,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        var membro = projetoService.adicionarMembro(id, requisicao, principal.getUsuarioId());
        return ResponseEntity.status(HttpStatus.CREATED).body(RespostaMembro.de(membro));
    }

    @DeleteMapping("/{id}/membros/{usuarioId}")
    public ResponseEntity<Void> removerMembro(
            @PathVariable Long id,
            @PathVariable Long usuarioId,
            @AuthenticationPrincipal UsuarioAutenticado principal) {
        projetoService.removerMembro(id, usuarioId, principal.getUsuarioId());
        return ResponseEntity.noContent().build();
    }
}
