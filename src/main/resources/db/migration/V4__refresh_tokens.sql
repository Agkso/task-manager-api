CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expira_em TIMESTAMP NOT NULL,
    revogado_em TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_usuario_id ON refresh_tokens(usuario_id);
