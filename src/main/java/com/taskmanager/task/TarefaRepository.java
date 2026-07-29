package com.taskmanager.task;

import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TarefaRepository extends JpaRepository<Tarefa, Long>, JpaSpecificationExecutor<Tarefa> {

    // sustenta a regra de WIP: no maximo 5 tarefas IN_PROGRESS por responsavel
    long countByResponsavelIdAndStatus(Long responsavelId, StatusTarefa status);

    @Query("select t.status as status, count(t) as total from Tarefa t where t.projeto.id = :projetoId group by t.status")
    List<ContagemStatus> contarPorStatus(@Param("projetoId") Long projetoId);

    @Query(
            "select t.prioridade as prioridade, count(t) as total from Tarefa t where t.projeto.id = :projetoId group by t.prioridade")
    List<ContagemPrioridade> contarPorPrioridade(@Param("projetoId") Long projetoId);

    interface ContagemStatus {
        StatusTarefa getStatus();

        long getTotal();
    }

    interface ContagemPrioridade {
        Prioridade getPrioridade();

        long getTotal();
    }
}
