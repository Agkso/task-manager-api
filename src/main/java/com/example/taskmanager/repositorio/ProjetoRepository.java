package com.example.taskmanager.repositorio;

import com.example.taskmanager.dominio.Projeto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {
}
