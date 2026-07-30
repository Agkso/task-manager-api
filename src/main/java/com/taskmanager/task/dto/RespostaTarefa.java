package com.taskmanager.task.dto;

import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import java.time.LocalDateTime;

/** Ver {@link com.taskmanager.task.TarefaMapper} para a conversao a partir de {@code Tarefa}. */
public record RespostaTarefa(
        Long id,
        Long projetoId,
        String titulo,
        String descricao,
        StatusTarefa status,
        Prioridade prioridade,
        LocalDateTime prazo,
        Long responsavelId,
        String responsavelNome,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {}
