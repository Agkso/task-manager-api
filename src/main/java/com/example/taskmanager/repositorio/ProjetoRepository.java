package com.example.taskmanager.repositorio;

import com.example.taskmanager.dominio.Projeto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

    List<Projeto> findByMembros_Usuario_Id(Long usuarioId);
}
