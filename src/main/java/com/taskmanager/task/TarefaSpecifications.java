package com.taskmanager.task;

import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import jakarta.persistence.criteria.JoinType;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

public final class TarefaSpecifications {

    private TarefaSpecifications() {}

    public static Specification<Tarefa> doProjeto(Long projetoId) {
        return (root, query, cb) -> cb.equal(root.get("projeto").get("id"), projetoId);
    }

    /**
     * Fetch join em responsavel: sem isso, listar tarefas com responsavel
     * definido gera 1 SELECT extra por linha (N+1) na hora de montar
     * RespostaTarefa, ja que Tarefa.responsavel e @ManyToOne(LAZY) e
     * findAll(Specification) nao aceita @EntityGraph como os metodos
     * derivados aceitam. So e seguro aplicar fetch join porque responsavel
     * e to-one (ManyToOne) - nao ha risco de multiplicar linhas como
     * haveria com uma colecao (OneToMany).
     *
     * O guard de resultType e defensivo: fetch join nao e permitido em
     * queries de COUNT. Hoje listar() so chama findAll(spec) sem Pageable,
     * entao nao ha count query - mas se isso mudar no futuro, essa
     * specification nao quebra a query de contagem.
     */
    public static Specification<Tarefa> comResponsavelCarregado() {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("responsavel", JoinType.LEFT);
            }
            return cb.conjunction();
        };
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
