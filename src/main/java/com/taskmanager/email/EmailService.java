package com.taskmanager.email;

/** Abstrai o provedor de email dos use cases que precisam notificar o usuario. */
public interface EmailService {

    void enviar(String destinatario, String assunto, String corpoHtml);
}
