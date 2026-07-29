package com.example.taskmanager.dto.autenticacao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequisicaoRegistro(
        @NotBlank(message = "nome e obrigatorio") String nome,
        @NotBlank(message = "email e obrigatorio") @Email(message = "email invalido") String email,
        @NotBlank(message = "senha e obrigatoria")
                @Size(min = 8, message = "senha deve ter no minimo 8 caracteres")
                String senha) {}
