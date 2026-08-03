package com.taskmanager.auth.passwordreset;

import com.taskmanager.exception.MensagensErro;
import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.user.Usuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Token opaco de uso unico terminal pra redefinicao de senha - mesma tecnica
 * de hash do RefreshTokenService (SHA-256 persistido, valor bruto so em
 * memoria e no link do email), mas sem rotacao: uma vez consumido
 * (validarEConsumir), o token nunca mais e' valido, ponto final.
 *
 * Expiracao bem mais curta que o refresh token (minutos, nao dias) de
 * proposito - um link de reset de senha que vaza (ex.: encaminhado sem
 * querer, cache de proxy corporativo) tem uma janela de exploracao muito
 * menor.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private static final int TAMANHO_TOKEN_BYTES = 64;
    private static final String ALGORITMO_HASH = "SHA-256";

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.password-reset.expiration-minutes}")
    private final long expiracaoMinutos;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String gerar(Usuario usuario) {
        String tokenBruto = tokenAleatorio();
        PasswordResetToken token = PasswordResetToken.builder()
                .usuario(usuario)
                .tokenHash(hash(tokenBruto))
                .expiraEm(LocalDateTime.now().plusMinutes(expiracaoMinutos))
                .build();
        passwordResetTokenRepository.save(token);
        return tokenBruto;
    }

    @Transactional
    public Usuario validarEConsumir(String tokenBruto) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHash(hash(tokenBruto))
                .orElseThrow(() -> new RegraNegocioException(MensagensErro.TOKEN_RESET_SENHA_INVALIDO));

        if (!token.valido()) {
            log.warn(
                    "Tentativa de uso de token de reset de senha invalido/expirado/ja usado (usuario {})",
                    token.getUsuario().getId());
            throw new RegraNegocioException(MensagensErro.TOKEN_RESET_SENHA_INVALIDO);
        }

        token.setUsadoEm(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
        return token.getUsuario();
    }

    private String tokenAleatorio() {
        byte[] bytes = new byte[TAMANHO_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String tokenBruto) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO_HASH);
            byte[] resultado = digest.digest(tokenBruto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resultado);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(MensagensErro.algoritmoHashIndisponivel(ALGORITMO_HASH), e);
        }
    }
}
