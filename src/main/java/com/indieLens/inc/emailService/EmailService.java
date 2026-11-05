package com.indieLens.inc.emailService;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Properties;

public class EmailService {

    private final String emailFrom;
    private final String appPassword;
    private final Session session;

    public EmailService(String emailFrom, String appPassword) {
        this.emailFrom = emailFrom;
        this.appPassword = appPassword;
        this.session = criarSessao();
    }

    private Session criarSessao() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailFrom, appPassword);
            }
        });

        return session;
    }

    public boolean enviarEmail(String destinatario, String titulo, String mensagem) {
        if (destinatario == null || destinatario.isBlank()) {
            System.out.println("Falha ao enviar email: usuário sem endereço de email.");
            return false;
        }

        try {
            InternetAddress endereco = new InternetAddress(destinatario);
            endereco.validate();

            MimeMessage message = new MimeMessage(this.session);
            message.setFrom(new InternetAddress(this.emailFrom));
            message.setRecipient(Message.RecipientType.TO, endereco);
            message.setSubject(titulo, "UTF-8");
            message.setText(mensagem, "UTF-8");

            Transport.send(message);
            System.out.println("E-mail enviado com sucesso para: " + destinatario);
            return true;

        } catch (MessagingException e) {
            System.out.println("Falha ao enviar e-mail para " + destinatario + ": " + e.getMessage());
            return false;
        }
    }

    public boolean enviarEmailEmLote(ArrayList destinatarios, String titulo, String mensagem) {
        boolean todosEnviados = !destinatarios.isEmpty();
        System.out.println("Iniciando envio em lote de " + destinatarios.size() + " e-mails.");

        for (Object item : destinatarios) {
            String email = (String) item;

            if (!enviarEmail(email, titulo, mensagem)) {
                todosEnviados = false;
            }
        }

        return todosEnviados;
    }

}
