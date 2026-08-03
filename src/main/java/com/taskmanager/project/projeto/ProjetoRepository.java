package com.taskmanager.project.projeto;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

    @EntityGraph(attributePaths = "dono")
    @Override
    Optional<Projeto> findById(Long id);

    // usado pelos services: projeto soft-deletado deve se comportar como inexistente (404)
    @EntityGraph(attributePaths = "dono")
    Optional<Projeto> findByIdAndExcluidoEmIsNull(Long id);

    @EntityGraph(attributePaths = "dono")
    List<Projeto> findByMembros_Usuario_Id(Long usuarioId);

    @EntityGraph(attributePaths = "dono")
    List<Projeto> findByMembros_Usuario_IdAndExcluidoEmIsNull(Long usuarioId);
}
