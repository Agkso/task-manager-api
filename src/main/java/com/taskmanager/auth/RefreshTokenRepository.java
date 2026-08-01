package com.taskmanager.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Remove tokens que ja nao servem pra nada: expirados ha mais de
     * {@code antesDe}, ou revogados ha mais de {@code antesDe}. Mantem
     * tokens expirados/revogados recentes por uma janela (retencao),
     * util so se algum dia precisar investigar uso indevido - depois
     * disso nao ha motivo pra guardar a linha.
     */
    @Modifying
    @Query("delete from RefreshToken t where t.expiraEm < :antesDe or t.revogadoEm < :antesDe")
    int excluirExpiradosOuRevogadosAntesDe(LocalDateTime antesDe);

    /**
     * Revoga todas as sessoes ativas de um usuario - usado ao redefinir
     * senha (ver RedefinirSenhaUseCase): se a troca de senha foi por causa
     * de uma conta comprometida, um refresh token que o atacante ja tenha
     * em maos continuaria valido depois da troca se isso nao existisse.
     */
    @Modifying
    @Query("update RefreshToken t set t.revogadoEm = CURRENT_TIMESTAMP where t.usuario.id = :usuarioId and t.revogadoEm is null")
    int revogarTodosDoUsuario(Long usuarioId);
}
