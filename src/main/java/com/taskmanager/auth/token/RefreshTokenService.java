package com.taskmanager.auth.token;

import com.taskmanager.exception.MensagensErro;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Token opaco (nao e JWT) persistido no banco, o que permite revogar antes
 * da expiracao - algo que um JWT sozinho nao permite sem uma denylist. So o
 * hash SHA-256 fica no banco (ver javadoc de RefreshToken); o valor bruto
 * so existe em memoria no momento da geracao/validacao e no corpo da
 * resposta HTTP.
 *
 * Rotacao em uso unico: cada refresh consome (revoga) o token atual e emite
 * um novo. Reusar um token ja revogado e tratado como token invalido, igual
 * a um token expirado - o cliente teria que logar de novo, mas isso e raro
 * (so acontece se um token vazar e for reusado por duas partes).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TAMANHO_TOKEN_BYTES = 64;
    private static final String ALGORITMO_HASH = "SHA-256";

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-days}")
    private final long expiracaoDias;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String gerar(Usuario usuario) {
        String tokenBruto = tokenAleatorio();
        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .tokenHash(hash(tokenBruto))
                .expiraEm(LocalDateTime.now().plusDays(expiracaoDias))
                .build();
        refreshTokenRepository.save(refreshToken);
        return tokenBruto;
    }

    @Transactional
    public RefreshToken validarERotacionar(String tokenBruto) {
        RefreshToken refreshToken = buscarValido(tokenBruto);
        refreshToken.setRevogadoEm(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    @Transactional
    public void revogarTodosDoUsuario(Long usuarioId) {
        refreshTokenRepository.revogarTodosDoUsuario(usuarioId);
    }

    @Transactional
    public void revogar(String tokenBruto) {
        RefreshToken refreshToken = buscarValido(tokenBruto);
        refreshToken.setRevogadoEm(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken buscarValido(String tokenBruto) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(hash(tokenBruto))
                .orElseThrow(() -> new BadCredentialsException(MensagensErro.REFRESH_TOKEN_INVALIDO));
        if (!refreshToken.valido()) {
            log.warn("Tentativa de uso de refresh token invalido/expirado/revogado (usuario {})",
                    refreshToken.getUsuario().getId());
            throw new BadCredentialsException(MensagensErro.REFRESH_TOKEN_INVALIDO);
        }
        return refreshToken;
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
