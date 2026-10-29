package com.indieLens.inc;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

import io.github.cdimascio.dotenv.Dotenv;

public class EmailSender {

    public static void main(String[] args) {
    	
    	//TODO :
    	
    	/*
    	1. Criar classe main
    	2. Criar métodos reutilizveis para envio de emails
    	3. re-utilizar sessão para envio (?) se for possivel claro 
    	*/
        
        // Carrega o arquivo .env da raiz do projeto
        Dotenv env = Dotenv.load();
        
        final String emailFrom = env.get("EMAIL_FROM");
        final String appPassword = env.get("APP_PASSWORD") != null ? env.get("APP_PASSWORD").replace(" ", "") : null;
        final String emailTo = env.get("EMAIL_TO");

        if (emailFrom == null || appPassword == null || emailTo == null) {
            System.err.println("ERRO: Uma ou mais variáveis do .env não foram encontradas!");
            return;
        }

        // 1. Configuração das propriedades SMTP
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        // 2. Criação da sessão
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailFrom, appPassword);
            }
        });
        
        // Ativa o log detalhado no console para identificar exatamente onde trava ou falha
        session.setDebug(true);

        try {
            System.out.println("Entrando na montagem MIME...");
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo));
            message.setSubject("Teste de Envio de E-mail via Java 21");
            message.setText("Olá! Este e-mail foi enviado utilizando Jakarta Mail no Java 21.");

            Transport.send(message);

            System.out.println("E-mail enviado com sucesso!");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}