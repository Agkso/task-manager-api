package com.taskmanager.task.dto;

import com.taskmanager.task.HistoricoTarefa;
import com.taskmanager.task.enums.StatusTarefa;
import java.time.LocalDateTime;

public record RespostaHistoricoTarefa(
        Long id,
        StatusTarefa statusAnterior,
        StatusTarefa statusNovo,
        String usuarioNome,
        LocalDateTime alteradoEm) {

    public static RespostaHistoricoTarefa de(HistoricoTarefa historico) {
        return new RespostaHistoricoTarefa(
                historico.getId(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getUsuario().getNome(),
                historico.getAlteradoEm());
    }
}
