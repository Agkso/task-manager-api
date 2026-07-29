package com.taskmanager.project;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

    @EntityGraph(attributePaths = "dono")
    @Override
    Optional<Projeto> findById(Long id);

    @EntityGraph(attributePaths = "dono")
    List<Projeto> findByMembros_Usuario_Id(Long usuarioId);
}
