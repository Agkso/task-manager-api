package com.taskmanager.task.historico;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoTarefaRepository extends JpaRepository<HistoricoTarefa, Long> {

    @EntityGraph(attributePaths = "usuario")
    List<HistoricoTarefa> findByTarefaIdOrderByAlteradoEmDesc(Long tarefaId);
}
