package com.taskmanager.task.dto;

import com.taskmanager.task.enums.StatusTarefa;

/** Payload enviado via SSE quando uma tarefa muda de status (ver TarefaEventoBroadcaster). */
public record RespostaEventoStatusTarefa(Long tarefaId, StatusTarefa statusAnterior, StatusTarefa statusNovo) {}
