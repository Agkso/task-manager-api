package com.example.taskmanager.controlador;

import com.example.taskmanager.dto.autenticacao.RequisicaoLogin;
import com.example.taskmanager.dto.autenticacao.RequisicaoRegistro;
import com.example.taskmanager.dto.autenticacao.RespostaLogin;
import com.example.taskmanager.servico.AutenticacaoService;
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
}
