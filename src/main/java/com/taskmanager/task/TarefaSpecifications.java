package com.taskmanager.task;

import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

public final class TarefaSpecifications {

    private TarefaSpecifications() {}

    public static Specification<Tarefa> doProjeto(Long projetoId) {
        return (root, query, cb) -> cb.equal(root.get("projeto").get("id"), projetoId);
    }

    public static Specification<Tarefa> comStatus(StatusTarefa status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Tarefa> comPrioridade(Prioridade prioridade) {
        return prioridade == null ? null : (root, query, cb) -> cb.equal(root.get("prioridade"), prioridade);
    }

    public static Specification<Tarefa> comResponsavel(Long responsavelId) {
        return responsavelId == null
                ? null
                : (root, query, cb) -> cb.equal(root.get("responsavel").get("id"), responsavelId);
    }

    public static Specification<Tarefa> comPrazoDesde(LocalDateTime desde) {
        return desde == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("prazo"), desde);
    }

    public static Specification<Tarefa> comPrazoAte(LocalDateTime ate) {
        return ate == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("prazo"), ate);
    }

    /**
     * Usa LOWER(coluna) LIKE pra casar com o indice funcional GIN trigram
     * criado em lower(titulo)/lower(descricao) (ver V2 da migration) - assim
     * a busca continua barata mesmo com a tabela de tarefas crescendo.
     */
    public static Specification<Tarefa> comTextoEm(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String padrao = "%" + texto.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("titulo")), padrao), cb.like(cb.lower(root.get("descricao")), padrao));
    }
}
