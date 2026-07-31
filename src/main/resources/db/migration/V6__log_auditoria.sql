-- Log de auditoria: ciclo de vida de projeto, membership e autenticacao.
-- Nao duplica historico_tarefa (que ja cobre mudanca de status de tarefa).
CREATE TABLE log_auditoria (
    id BIGSERIAL PRIMARY KEY,
    acao VARCHAR(40) NOT NULL,
    tipo_entidade VARCHAR(40) NOT NULL,
    entidade_id BIGINT,
    projeto_id BIGINT REFERENCES projetos(id),
    usuario_id BIGINT REFERENCES usuarios(id),
    detalhe VARCHAR(500),
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_log_auditoria_projeto_id ON log_auditoria(projeto_id, criado_em DESC);
