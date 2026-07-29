-- V1 criou os indices trigram nas colunas cruas (titulo/descricao), mas a
-- busca textual usa LOWER(coluna) LIKE '%termo%' pra ser case-insensitive.
-- Um indice em coluna crua nao e usado por uma consulta com LOWER() na
-- clausula WHERE - precisa ser um indice funcional na mesma expressao.
DROP INDEX IF EXISTS idx_tarefas_titulo_trgm;
DROP INDEX IF EXISTS idx_tarefas_descricao_trgm;

CREATE INDEX idx_tarefas_titulo_trgm ON tarefas USING GIN (lower(titulo) gin_trgm_ops);
CREATE INDEX idx_tarefas_descricao_trgm ON tarefas USING GIN (lower(descricao) gin_trgm_ops);
