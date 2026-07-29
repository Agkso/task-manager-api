package com.example.taskmanager.repositorio;

import com.example.taskmanager.dominio.MembroProjeto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembroProjetoRepository extends JpaRepository<MembroProjeto, Long> {

    Optional<MembroProjeto> findByProjetoIdAndUsuarioId(Long projetoId, Long usuarioId);

    List<MembroProjeto> findByProjetoId(Long projetoId);

    List<MembroProjeto> findByUsuarioId(Long usuarioId);

    boolean existsByProjetoIdAndUsuarioId(Long projetoId, Long usuarioId);
}
