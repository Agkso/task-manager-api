package com.taskmanager.auth.usecase;

import com.taskmanager.auth.token.RefreshTokenService;
import com.taskmanager.auth.dto.RequisicaoRefreshToken;
import com.taskmanager.auth.dto.RespostaLogin;
import com.taskmanager.security.JwtService;
import com.taskmanager.user.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Troca um refresh token valido por um novo access token, rotacionando o refresh token em uso unico. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RenovarTokenUseCase {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public RespostaLogin executar(RequisicaoRefreshToken requisicao) {
        var refreshToken = refreshTokenService.validarERotacionar(requisicao.refreshToken());
        Usuario usuario = refreshToken.getUsuario();
        log.info("Access token renovado via refresh token para usuario {}", usuario.getId());
        return new RespostaLogin(jwtService.gerarToken(usuario), refreshTokenService.gerar(usuario));
    }
}
