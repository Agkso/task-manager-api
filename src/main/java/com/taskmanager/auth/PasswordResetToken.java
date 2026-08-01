package com.taskmanager.auth;

import com.taskmanager.user.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mesmo design do {@link RefreshToken} (so o hash SHA-256 fica no banco,
 * nunca o valor bruto) - a diferenca e' o ciclo de vida: um refresh token e'
 * rotacionado a cada uso (ver RefreshTokenService), um token de reset de
 * senha e' de uso unico terminal - depois de consumido (usadoEm preenchido)
 * ele nunca mais serve pra nada, entao nao ha "rotacionar", so "consumir".
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "usado_em")
    private LocalDateTime usadoEm;

    public boolean valido() {
        return usadoEm == null && expiraEm.isAfter(LocalDateTime.now());
    }
}
