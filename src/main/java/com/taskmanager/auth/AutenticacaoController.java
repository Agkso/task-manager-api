package com.taskmanager.auth;

import com.taskmanager.auth.dto.RequisicaoLogin;
import com.taskmanager.auth.dto.RequisicaoRefreshToken;
import com.taskmanager.auth.dto.RequisicaoRegistro;
import com.taskmanager.auth.dto.RespostaLogin;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    public AutenticacaoController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping("/registrar")
    public ResponseEntity<RespostaLogin> registrar(@Valid @RequestBody RequisicaoRegistro requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(autenticacaoService.registrar(requisicao));
    }

    @PostMapping("/login")
    public ResponseEntity<RespostaLogin> login(@Valid @RequestBody RequisicaoLogin requisicao) {
        return ResponseEntity.ok(autenticacaoService.autenticar(requisicao));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RespostaLogin> renovar(@Valid @RequestBody RequisicaoRefreshToken requisicao) {
        return ResponseEntity.ok(autenticacaoService.renovar(requisicao));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RequisicaoRefreshToken requisicao) {
        autenticacaoService.logout(requisicao);
        return ResponseEntity.noContent().build();
    }
}
