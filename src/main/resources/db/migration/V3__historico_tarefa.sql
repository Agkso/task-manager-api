CREATE TABLE historico_tarefa (
    id BIGSERIAL PRIMARY KEY,
    tarefa_id BIGINT NOT NULL REFERENCES tarefas(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    status_anterior VARCHAR(20) NOT NULL,
    status_novo VARCHAR(20) NOT NULL,
    alterado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_historico_tarefa_tarefa_id ON historico_tarefa(tarefa_id);
