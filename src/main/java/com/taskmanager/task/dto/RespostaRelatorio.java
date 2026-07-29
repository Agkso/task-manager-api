package com.taskmanager.task.dto;

import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import java.util.Map;

// nomes dos campos (byStatus/byPriority) seguem literalmente o exemplo de JSON do enunciado
public record RespostaRelatorio(Map<StatusTarefa, Long> byStatus, Map<Prioridade, Long> byPriority) {}
