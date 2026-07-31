-- Soft delete em projeto e tarefa: exclusao passa a marcar excluido_em em vez
-- de fazer DELETE fisico, preservando historico (historico_tarefa tinha
-- ON DELETE CASCADE em tarefa_id - um DELETE fisico de uma tarefa apagava a
-- propria auditoria dela junto) e permitindo desfazer uma exclusao por engano.
ALTER TABLE projetos ADD COLUMN excluido_em TIMESTAMP;
ALTER TABLE tarefas ADD COLUMN excluido_em TIMESTAMP;
