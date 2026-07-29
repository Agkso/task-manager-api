package com.taskmanager.task.dto;

import com.taskmanager.task.enums.StatusTarefa;
import jakarta.validation.constraints.NotNull;

public record RequisicaoAtualizarStatus(@NotNull(message = "status e obrigatorio") StatusTarefa status) {}
