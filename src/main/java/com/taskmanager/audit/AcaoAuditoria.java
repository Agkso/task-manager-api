package com.taskmanager.audit;

/**
 * Enum fechado das acoes auditadas, em vez de String livre no call site -
 * evita literais divergentes (ex.: "PROJETO_CRIADO" vs "PROJETO_CRIAR") entre
 * os ~10 pontos de publicacao.
 */
public enum AcaoAuditoria {
    PROJETO_CRIADO,
    PROJETO_ATUALIZADO,
    PROJETO_EXCLUIDO,
    MEMBRO_ADICIONADO,
    MEMBRO_REMOVIDO,
    TAREFA_CRIADA,
    TAREFA_ATUALIZADA,
    TAREFA_EXCLUIDA,
    USUARIO_REGISTRADO,
    LOGIN_SUCEDIDO
}
