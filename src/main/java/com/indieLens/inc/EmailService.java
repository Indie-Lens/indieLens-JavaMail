package com.indieLens.inc;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;

public class EmailService {

    private final String emailFrom;
    private final String appPassword;
    private Session session;

    public EmailService(String emailFrom, String appPassword) {
        this.emailFrom = emailFrom;
        this.appPassword = appPassword;
        this.session = criarSessao();
    }

    private Session criarSessao() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailFrom, appPassword);
            }
        });

        // Ativa logs detalhados para depuração
        session.setDebug(true);
        return session;
    }

    public void enviarEmail(String destinatario, String titulo, String mensagem) {
        try {
            Message message = new MimeMessage(this.session);
            message.setFrom(new InternetAddress(this.emailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject(titulo);
            message.setText(mensagem);

            Transport.send(message);
            System.out.println("E-mail enviado com sucesso para: " + destinatario);

        } catch (MessagingException e) {
            System.err.println("Falha ao enviar e-mail para " + destinatario + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void enviarEmailEmLote(List<String> destinatarios, String titulo, String mensagem) {
        System.out.println("Iniciando envio em lote de " + destinatarios.size() + " e-mails.");
        for (String email : destinatarios) {
            enviarEmail(email, titulo, mensagem);
        }
        System.out.println("Envio em lote concluído!");
    }

}