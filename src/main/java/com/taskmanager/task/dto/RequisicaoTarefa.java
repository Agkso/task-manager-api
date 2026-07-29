package com.taskmanager.task.dto;

import com.taskmanager.task.enums.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record RequisicaoTarefa(
        @NotBlank(message = "titulo e obrigatorio") @Size(max = 255) String titulo,
        @Size(max = 4000) String descricao,
        @NotNull(message = "prioridade e obrigatoria") Prioridade prioridade,
        LocalDateTime prazo,
        Long responsavelId) {}
