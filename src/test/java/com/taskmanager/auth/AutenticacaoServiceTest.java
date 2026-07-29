package com.taskmanager.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taskmanager.auth.dto.RequisicaoRegistro;
import com.taskmanager.exception.RegraNegocioException;
import com.taskmanager.security.JwtService;
import com.taskmanager.user.Usuario;
import com.taskmanager.user.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AutenticacaoService autenticacaoService;

    @BeforeEach
    void montarService() {
        autenticacaoService =
                new AutenticacaoService(usuarioRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void registrar_deveRejeitarQuandoEmailJaCadastrado() {
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(true);

        RequisicaoRegistro requisicao = new RequisicaoRegistro("Ana", "ana@example.com", "senha1234");

        assertThatThrownBy(() -> autenticacaoService.registrar(requisicao))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("email");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_deveCriarUsuarioEGerarTokenQuandoEmailNovo() {
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("hash-fake");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(jwtService.gerarToken(any(Usuario.class))).thenReturn("token-fake");

        RequisicaoRegistro requisicao = new RequisicaoRegistro("Ana", "ana@example.com", "senha1234");
        var resposta = autenticacaoService.registrar(requisicao);

        assertThat(resposta.token()).isEqualTo("token-fake");
        verify(passwordEncoder).encode("senha1234");
    }
}
