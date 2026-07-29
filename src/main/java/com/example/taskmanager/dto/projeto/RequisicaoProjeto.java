package com.example.taskmanager.dto.projeto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequisicaoProjeto(
        @NotBlank(message = "nome e obrigatorio") @Size(max = 255) String nome,
        @Size(max = 2000, message = "descricao deve ter no maximo 2000 caracteres") String descricao) {}
