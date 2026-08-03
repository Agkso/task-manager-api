package com.taskmanager.task.tarefa;

import com.taskmanager.task.enums.Prioridade;
import com.taskmanager.task.enums.StatusTarefa;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TarefaRepository extends JpaRepository<Tarefa, Long>, JpaSpecificationExecutor<Tarefa> {

    // evita lazy load de responsavel/projeto (ambos @ManyToOne LAZY) ao montar RespostaTarefa
    @EntityGraph(attributePaths = {"responsavel", "projeto"})
    @Override
    Optional<Tarefa> findById(Long id);

    // usado pelos use cases (via TarefaHelper): tarefa soft-deletada deve se comportar como inexistente
    @EntityGraph(attributePaths = {"responsavel", "projeto"})
    Optional<Tarefa> findByIdAndExcluidoEmIsNull(Long id);

    // sustenta a regra de WIP: no maximo 5 tarefas IN_PROGRESS por responsavel; exclui tarefas
    // soft-deletadas da contagem, senao uma tarefa "excluida" ainda ocuparia vaga de WIP
    long countByResponsavelIdAndStatusAndExcluidoEmIsNull(Long responsavelId, StatusTarefa status);

    @Query(
            "select t.status as status, count(t) as total from Tarefa t where t.projeto.id = :projetoId and t.excluidoEm is null group by t.status")
    List<ContagemStatus> contarPorStatus(@Param("projetoId") Long projetoId);

    @Query(
            "select t.prioridade as prioridade, count(t) as total from Tarefa t where t.projeto.id = :projetoId and t.excluidoEm is null group by t.prioridade")
    List<ContagemPrioridade> contarPorPrioridade(@Param("projetoId") Long projetoId);

    @Modifying
    @Query("update Tarefa t set t.excluidoEm = :agora where t.projeto.id = :projetoId and t.excluidoEm is null")
    int softDeleteByProjetoId(@Param("projetoId") Long projetoId, @Param("agora") LocalDateTime agora);

    interface ContagemStatus {
        StatusTarefa getStatus();

        long getTotal();
    }

    interface ContagemPrioridade {
        Prioridade getPrioridade();

        long getTotal();
    }
}
