package com.taskmanager.auth;

import com.taskmanager.auth.dto.RequisicaoEsqueciSenha;
import com.taskmanager.auth.dto.RequisicaoLogin;
import com.taskmanager.auth.dto.RequisicaoRedefinirSenha;
import com.taskmanager.auth.dto.RequisicaoRefreshToken;
import com.taskmanager.auth.dto.RequisicaoRegistro;
import com.taskmanager.auth.dto.RespostaLogin;
import com.taskmanager.auth.usecase.AutenticarUsuarioUseCase;
import com.taskmanager.auth.usecase.LogoutUseCase;
import com.taskmanager.auth.usecase.RedefinirSenhaUseCase;
import com.taskmanager.auth.usecase.RegistrarUsuarioUseCase;
import com.taskmanager.auth.usecase.RenovarTokenUseCase;
import com.taskmanager.auth.usecase.SolicitarRedefinicaoSenhaUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final AutenticarUsuarioUseCase autenticarUsuarioUseCase;
    private final RenovarTokenUseCase renovarTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final SolicitarRedefinicaoSenhaUseCase solicitarRedefinicaoSenhaUseCase;
    private final RedefinirSenhaUseCase redefinirSenhaUseCase;

    @PostMapping("/registrar")
    public ResponseEntity<RespostaLogin> registrar(@Valid @RequestBody RequisicaoRegistro requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrarUsuarioUseCase.executar(requisicao));
    }

    @PostMapping("/login")
    public ResponseEntity<RespostaLogin> login(@Valid @RequestBody RequisicaoLogin requisicao) {
        return ResponseEntity.ok(autenticarUsuarioUseCase.executar(requisicao));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RespostaLogin> renovar(@Valid @RequestBody RequisicaoRefreshToken requisicao) {
        return ResponseEntity.ok(renovarTokenUseCase.executar(requisicao));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RequisicaoRefreshToken requisicao) {
        logoutUseCase.executar(requisicao);
        return ResponseEntity.noContent().build();
    }

    /**
     * Sempre 202/void, exista ou nao o email - ver javadoc de
     * SolicitarRedefinicaoSenhaUseCase pra o porque (enumeration).
     */
    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> esqueciSenha(@Valid @RequestBody RequisicaoEsqueciSenha requisicao) {
        solicitarRedefinicaoSenhaUseCase.executar(requisicao);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RequisicaoRedefinirSenha requisicao) {
        redefinirSenhaUseCase.executar(requisicao);
        return ResponseEntity.noContent().build();
    }
}
