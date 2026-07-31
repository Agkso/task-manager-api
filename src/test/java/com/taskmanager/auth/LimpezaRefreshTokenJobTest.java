package com.taskmanager.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LimpezaRefreshTokenJobTest {

    private static final long RETENCAO_DIAS = 30;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private LimpezaRefreshTokenJob limpezaRefreshTokenJob;

    @BeforeEach
    void montarJob() {
        limpezaRefreshTokenJob = new LimpezaRefreshTokenJob(refreshTokenRepository, RETENCAO_DIAS);
    }

    @Test
    void limpar_deveDelegarParaORepositorioComACorteDeRetencao() {
        when(refreshTokenRepository.excluirExpiradosOuRevogadosAntesDe(any())).thenReturn(3);

        limpezaRefreshTokenJob.limpar();

        verify(refreshTokenRepository).excluirExpiradosOuRevogadosAntesDe(any());
    }
}
