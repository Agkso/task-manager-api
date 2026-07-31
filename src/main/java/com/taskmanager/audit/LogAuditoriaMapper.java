package com.taskmanager.audit;

import com.taskmanager.audit.dto.RespostaLogAuditoria;
import org.springframework.stereotype.Component;

@Component
public class LogAuditoriaMapper {

    public RespostaLogAuditoria paraResposta(LogAuditoria log) {
        return new RespostaLogAuditoria(
                log.getId(),
                log.getAcao(),
                log.getTipoEntidade(),
                log.getEntidadeId(),
                log.getUsuarioId(),
                log.getDetalhe(),
                log.getCriadoEm());
    }
}
