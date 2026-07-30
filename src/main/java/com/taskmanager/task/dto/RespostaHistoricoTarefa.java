package com.taskmanager.task.dto;

import com.taskmanager.task.enums.StatusTarefa;
import java.time.LocalDateTime;

/** Ver {@link com.taskmanager.task.HistoricoTarefaMapper} para a conversao a partir de {@code HistoricoTarefa}. */
public record RespostaHistoricoTarefa(
        Long id, StatusTarefa statusAnterior, StatusTarefa statusNovo, String usuarioNome, LocalDateTime alteradoEm) {}
