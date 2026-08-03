package com.taskmanager.project.membro;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembroProjetoRepository extends JpaRepository<MembroProjeto, Long> {

    @EntityGraph(attributePaths = "usuario")
    Optional<MembroProjeto> findByProjetoIdAndUsuarioId(Long projetoId, Long usuarioId);

    // usado por obterMembro: projeto soft-deletado deve barrar acesso como se o
    // solicitante nunca tivesse sido membro (mesmo efeito de "nao encontrado")
    @EntityGraph(attributePaths = "usuario")
    Optional<MembroProjeto> findByProjetoIdAndUsuarioIdAndProjeto_ExcluidoEmIsNull(Long projetoId, Long usuarioId);

    @EntityGraph(attributePaths = "usuario")
    List<MembroProjeto> findByProjetoId(Long projetoId);

    List<MembroProjeto> findByUsuarioId(Long usuarioId);

    boolean existsByProjetoIdAndUsuarioId(Long projetoId, Long usuarioId);
}
