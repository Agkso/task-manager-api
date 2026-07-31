package com.taskmanager.audit.dto;

import java.time.LocalDateTime;

public record RespostaLogAuditoria(
        Long id,
        String acao,
        String tipoEntidade,
        Long entidadeId,
        Long usuarioId,
        String detalhe,
        LocalDateTime criadoEm) {}
