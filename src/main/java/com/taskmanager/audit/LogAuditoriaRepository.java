package com.taskmanager.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    Page<LogAuditoria> findByProjetoIdOrderByCriadoEmDesc(Long projetoId, Pageable pageable);
}
