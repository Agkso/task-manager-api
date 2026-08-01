package com.taskmanager.email;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Integracao via API HTTP do Resend (nao SMTP) - um POST simples com a API
 * key no header, sem biblioteca extra alem do RestClient que ja vem no
 * spring-boot-starter-web.
 *
 * Falha de envio nunca propaga pro chamador (so loga): o use case que chama
 * isso (SolicitarRedefinicaoSenhaUseCase) precisa devolver a mesma resposta
 * pro cliente independente do email existir ou o envio ter funcionado - senao
 * um erro 500 so quando o email existe (e o envio falha) vazaria quais
 * emails tem conta (enumeration). Se RESEND_API_KEY nao estiver configurada
 * (dev/CI sem credencial), so loga o que mandaria em vez de quebrar a
 * aplicacao.
 */
@Slf4j
@Service
public class ResendEmailService implements EmailService {

    private final RestClient restClient;
    private final String remetente;
    private final boolean configurado;

    public ResendEmailService(
            @Value("${app.email.resend-api-key}") String apiKey, @Value("${app.email.remetente}") String remetente) {
        this.remetente = remetente;
        this.configurado = apiKey != null && !apiKey.isBlank();
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public void enviar(String destinatario, String assunto, String corpoHtml) {
        if (!configurado) {
            log.warn(
                    "RESEND_API_KEY nao configurada - email para {} nao foi enviado (assunto: {})",
                    destinatario,
                    assunto);
            return;
        }

        try {
            restClient
                    .post()
                    .uri("/emails")
                    .body(Map.of(
                            "from", remetente,
                            "to", List.of(destinatario),
                            "subject", assunto,
                            "html", corpoHtml))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email enviado via Resend para {}", destinatario);
        } catch (RestClientException e) {
            log.error("Falha ao enviar email via Resend para {}", destinatario, e);
        }
    }
}
