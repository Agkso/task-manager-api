package com.example.taskmanager.servico;

import com.example.taskmanager.dominio.Usuario;
import com.example.taskmanager.dto.autenticacao.RequisicaoLogin;
import com.example.taskmanager.dto.autenticacao.RequisicaoRegistro;
import com.example.taskmanager.dto.autenticacao.RespostaLogin;
import com.example.taskmanager.excecao.RegraNegocioException;
import com.example.taskmanager.repositorio.UsuarioRepository;
import com.example.taskmanager.seguranca.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AutenticacaoService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public RespostaLogin registrar(RequisicaoRegistro requisicao) {
        if (usuarioRepository.existsByEmail(requisicao.email())) {
            throw new RegraNegocioException("Ja existe um usuario cadastrado com esse email");
        }

        Usuario usuario = Usuario.builder()
                .nome(requisicao.nome())
                .email(requisicao.email())
                .senha(passwordEncoder.encode(requisicao.senha()))
                .build();

        usuarioRepository.save(usuario);
        return new RespostaLogin(jwtService.gerarToken(usuario));
    }

    public RespostaLogin autenticar(RequisicaoLogin requisicao) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requisicao.email(), requisicao.senha()));

        Usuario usuario = usuarioRepository
                .findByEmail(requisicao.email())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado nao encontrado: "
                        + requisicao.email()));

        return new RespostaLogin(jwtService.gerarToken(usuario));
    }
}
