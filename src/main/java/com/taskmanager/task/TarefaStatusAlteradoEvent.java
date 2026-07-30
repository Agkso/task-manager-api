package com.taskmanager.task;

import com.taskmanager.task.enums.StatusTarefa;

public record TarefaStatusAlteradoEvent(
        Long tarefaId, Long usuarioId, StatusTarefa statusAnterior, StatusTarefa statusNovo) {}
