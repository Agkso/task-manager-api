package com.example.taskmanager.dto.autenticacao;

import jakarta.validation.constraints.NotBlank;

public record RequisicaoLogin(
        @NotBlank(message = "email e obrigatorio") String email,
        @NotBlank(message = "senha e obrigatoria") String senha) {}
