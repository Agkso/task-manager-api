package com.taskmanager.project.dto;

import com.taskmanager.project.enums.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequisicaoAdicionarMembro(
        @NotBlank(message = "email e obrigatorio") @Email(message = "email invalido") String email,
        @NotNull(message = "papel e obrigatorio") Papel papel) {}
