package com.taskmanager.task;

import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import jakarta.persistence.criteria.Expression;
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
     * O guard de resultType e necessario de verdade agora que listar() usa
     * findAll(spec, Pageable): Pageable gera uma query de COUNT separada
     * (resultType Long) alem da query de conteudo, e fetch join nao e
     * permitido em query de COUNT.
     */
    public static Specification<Tarefa> comResponsavelCarregado() {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("responsavel", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    /**
     * Ordenacao por prioridade nao da pra expressar com Sort.by(...) comum
     * (a ordem alfabetica do enum nao e a ordem de severidade) nem com
     * JpaSort.unsafe() - essa API so funciona em queries derivadas por nome
     * de metodo, que geram JPQL por concatenacao de string. Specification
     * usa Criteria API, que resolve Sort via PropertyPath (precisa ser uma
     * propriedade de verdade da entidade). A saida e montar o CASE WHEN
     * direto no CriteriaBuilder e chamar query.orderBy(...) aqui dentro -
     * funciona porque o Specification roda antes do Sort ser aplicado, e o
     * service passa Sort.unsorted() nesse caso pra nao sobrescrever isso.
     */
    public static Specification<Tarefa> ordenarPorPrioridade(boolean descendente) {
        return (root, query, cb) -> {
            Expression<Integer> peso = cb.<Prioridade, Integer>selectCase(root.get("prioridade"))
                    .when(Prioridade.LOW, 1)
                    .when(Prioridade.MEDIUM, 2)
                    .when(Prioridade.HIGH, 3)
                    .when(Prioridade.CRITICAL, 4)
                    .otherwise(0);
            query.orderBy(descendente ? cb.desc(peso) : cb.asc(peso));
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
