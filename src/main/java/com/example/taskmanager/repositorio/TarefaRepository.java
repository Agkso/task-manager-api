package com.example.taskmanager.repositorio;

import com.example.taskmanager.dominio.Tarefa;
import com.example.taskmanager.dominio.enums.StatusTarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TarefaRepository extends JpaRepository<Tarefa, Long>, JpaSpecificationExecutor<Tarefa> {

    // sustenta a regra de WIP: no maximo 5 tarefas IN_PROGRESS por responsavel
    long countByResponsavelIdAndStatus(Long responsavelId, StatusTarefa status);
}
