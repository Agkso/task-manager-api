package com.taskmanager.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Mesma logica de retencao do RefreshTokenRepository - ver excluirExpiradosOuRevogadosAntesDe. */
    @Modifying
    @Query("delete from PasswordResetToken t where t.expiraEm < :antesDe or t.usadoEm < :antesDe")
    int excluirExpiradosOuUsadosAntesDe(LocalDateTime antesDe);
}
