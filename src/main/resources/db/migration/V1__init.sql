CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE projetos (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    descricao VARCHAR(2000),
    dono_id BIGINT NOT NULL REFERENCES usuarios(id),
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE membros_projeto (
    id BIGSERIAL PRIMARY KEY,
    projeto_id BIGINT NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    papel VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_membros_projeto_projeto_usuario UNIQUE (projeto_id, usuario_id)
);

CREATE INDEX idx_membros_projeto_usuario_id ON membros_projeto(usuario_id);

CREATE TABLE tarefas (
    id BIGSERIAL PRIMARY KEY,
    projeto_id BIGINT NOT NULL REFERENCES projetos(id) ON DELETE CASCADE,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(4000),
    status VARCHAR(20) NOT NULL,
    prioridade VARCHAR(20) NOT NULL,
    prazo TIMESTAMP,
    responsavel_id BIGINT REFERENCES usuarios(id),
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_tarefas_projeto_id ON tarefas(projeto_id);
CREATE INDEX idx_tarefas_status ON tarefas(status);
CREATE INDEX idx_tarefas_prioridade ON tarefas(prioridade);
CREATE INDEX idx_tarefas_prazo ON tarefas(prazo);

-- mantem a checagem de limite de WIP (max 5 tarefas IN_PROGRESS por responsavel) como um lookup rapido via indice
CREATE INDEX idx_tarefas_responsavel_status ON tarefas(responsavel_id, status);

-- indices trigram para a busca textual em titulo/descricao nao virar um seq scan completo
CREATE INDEX idx_tarefas_titulo_trgm ON tarefas USING GIN (titulo gin_trgm_ops);
CREATE INDEX idx_tarefas_descricao_trgm ON tarefas USING GIN (descricao gin_trgm_ops);
