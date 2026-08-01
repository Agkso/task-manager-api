package com.taskmanager.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequisicaoRedefinirSenha(
        @NotBlank(message = "token e obrigatorio") String token,
        @NotBlank(message = "novaSenha e obrigatoria")
                @Size(min = 8, message = "novaSenha deve ter no minimo 8 caracteres")
                String novaSenha) {}
