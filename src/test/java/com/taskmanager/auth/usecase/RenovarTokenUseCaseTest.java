package com.taskmanager.auth.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.taskmanager.auth.RefreshToken;
import com.taskmanager.auth.RefreshTokenService;
import com.taskmanager.auth.dto.RequisicaoRefreshToken;
import com.taskmanager.security.JwtService;
import com.taskmanager.user.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class RenovarTokenUseCaseTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private RenovarTokenUseCase renovarTokenUseCase;

    @BeforeEach
    void montarUseCase() {
        renovarTokenUseCase = new RenovarTokenUseCase(jwtService, refreshTokenService);
    }

    @Test
    void executar_deveGerarNovoAccessTokenERotacionarRefreshToken() {
        Usuario usuario = Usuario.builder().id(1L).email("ana@example.com").build();
        RefreshToken refreshTokenValido =
                RefreshToken.builder().usuario(usuario).build();
        when(refreshTokenService.validarERotacionar("token-antigo")).thenReturn(refreshTokenValido);
        when(jwtService.gerarToken(usuario)).thenReturn("token-novo");
        when(refreshTokenService.gerar(usuario)).thenReturn("refresh-novo");

        var resposta = renovarTokenUseCase.executar(new RequisicaoRefreshToken("token-antigo"));

        assertThat(resposta.token()).isEqualTo("token-novo");
        assertThat(resposta.refreshToken()).isEqualTo("refresh-novo");
    }

    @Test
    void executar_devePropagarErroQuandoRefreshTokenInvalido() {
        when(refreshTokenService.validarERotacionar("token-invalido"))
                .thenThrow(new BadCredentialsException("Refresh token invalido"));

        assertThatThrownBy(() -> renovarTokenUseCase.executar(new RequisicaoRefreshToken("token-invalido")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
