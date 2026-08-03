package com.taskmanager.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmanager.user.Usuario;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long EXPIRACAO_DIAS = 7;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void montarService() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, EXPIRACAO_DIAS);
    }

    private Usuario usuario() {
        return Usuario.builder().id(1L).email("ana@example.com").build();
    }

    @Test
    void gerar_deveRetornarTokenBrutoDiferenteDoHashPersistido() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(chamada -> chamada.getArgument(0));

        String tokenBruto = refreshTokenService.gerar(usuario());

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        String hashPersistido = refreshTokenCaptor.getValue().getTokenHash();

        assertThat(tokenBruto).isNotBlank();
        assertThat(hashPersistido).isNotBlank().isNotEqualTo(tokenBruto);
    }

    @Test
    void validarERotacionar_deveRejeitarTokenInexistente() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validarERotacionar("token-qualquer"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void validarERotacionar_deveRejeitarTokenJaRevogado() {
        RefreshToken revogado = RefreshToken.builder()
                .usuario(usuario())
                .expiraEm(LocalDateTime.now().plusDays(1))
                .revogadoEm(LocalDateTime.now().minusMinutes(1))
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revogado));

        assertThatThrownBy(() -> refreshTokenService.validarERotacionar("token-reusado"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void validarERotacionar_deveRejeitarTokenExpirado() {
        RefreshToken expirado = RefreshToken.builder()
                .usuario(usuario())
                .expiraEm(LocalDateTime.now().minusSeconds(1))
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expirado));

        assertThatThrownBy(() -> refreshTokenService.validarERotacionar("token-expirado"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void validarERotacionar_deveMarcarTokenComoRevogadoQuandoValido() {
        RefreshToken valido = RefreshToken.builder()
                .usuario(usuario())
                .expiraEm(LocalDateTime.now().plusDays(1))
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(valido));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(chamada -> chamada.getArgument(0));

        RefreshToken resultado = refreshTokenService.validarERotacionar("token-valido");

        assertThat(resultado.getRevogadoEm()).isNotNull();
    }
}
