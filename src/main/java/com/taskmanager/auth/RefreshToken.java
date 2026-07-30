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
 * Guarda o hash SHA-256 do token, nunca o valor bruto - equivalente a como
 * senha e tratada com BCrypt: se o banco vazar, o token em si nao da pra
 * reusar. Rotacao (ver RefreshTokenService.validarERotacionar) marca
 * revogadoEm no token antigo a cada uso, entao um token roubado e reusado
 * pelo atacante depois do dono ja invalida os dois - o dono percebe no
 * proximo refresh que falhou.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

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

    @Column(name = "revogado_em")
    private LocalDateTime revogadoEm;

    public boolean valido() {
        return revogadoEm == null && expiraEm.isAfter(LocalDateTime.now());
    }
}
