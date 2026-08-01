package com.taskmanager.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequisicaoEsqueciSenha(
        @NotBlank(message = "email e obrigatorio") @Email(message = "email invalido") String email) {}
