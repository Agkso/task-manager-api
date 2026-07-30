package com.taskmanager.auth.usecase;

import com.taskmanager.auth.RefreshTokenService;
import com.taskmanager.auth.dto.RequisicaoRefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Revoga o refresh token informado, encerrando a sessao no dispositivo atual. */
@Component
@RequiredArgsConstructor
public class LogoutUseCase {

    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void executar(RequisicaoRefreshToken requisicao) {
        refreshTokenService.revogar(requisicao.refreshToken());
    }
}
