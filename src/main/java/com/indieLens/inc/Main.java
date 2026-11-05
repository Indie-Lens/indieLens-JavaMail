package com.indieLens.inc;

import com.indieLens.inc.Database.Conexao;
import com.indieLens.inc.Database.Methods;
import com.indieLens.inc.emailService.EmailService;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        SingletonEvent evento = SingletonEvent.getInstance();

        try {
            if (!evento.iniciar()) {
                System.out.println("O serviço já está em execução. Esta execução será ignorada.");
                return;
            }
        } catch (IOException e) {
            System.out.println("Erro ao criar a trava de execução: " + e.getMessage());
            return;
        }

        try {
            Dotenv env = Dotenv.configure().ignoreIfMissing().load();
            Conexao conexao;

            try {
                conexao = new Conexao(env);
                conexao.checarConexao();
                System.out.println("Conexão com o banco de dados realizada com sucesso.");
            } catch (SQLException | RuntimeException e) {
                System.out.println("Erro ao conectar ao banco de dados: " + e.getMessage());
                return;
            }

            Methods metodos = new Methods(conexao.getConexao());
            ArrayList notificacoes = metodos.prepararNotificacoes();

            if (notificacoes.isEmpty()) {
                System.out.println("Nenhuma notificação pendente para enviar.");
                return;
            }

            String emailFrom = env.get("EMAIL_FROM");
            String appPassword = env.get("APP_PASSWORD");

            if (appPassword != null) {
                appPassword = appPassword.replace(" ", "");
            }

            if (emailFrom == null || emailFrom.isBlank() || appPassword == null || appPassword.isBlank()) {
                System.out.println("Erro: configure EMAIL_FROM e APP_PASSWORD para enviar os emails.");
                return;
            }

            EmailService emailService = new EmailService(emailFrom, appPassword);
            processarNotificacoes(notificacoes, metodos, emailService);
        } catch (SQLException | RuntimeException e) {
            System.out.println("Erro ao processar as notificações: " + e.getMessage());
        } finally {
            try {
                evento.finalizar();
            } catch (IOException e) {
                System.out.println("Erro ao liberar a trava de execução: " + e.getMessage());
            }

            System.out.println("Processamento de emails finalizado.");
        }
    }

    public static void processarNotificacoes(ArrayList notificacoes, Methods metodos, EmailService emailService) {
        for (Object item : notificacoes) {
            Notificacao notificacao = (Notificacao) item;

            if (notificacao.getEmails().isEmpty()) {
                System.out.println("Notificação " + notificacao.getIdNotificacao()
                        + " sem destinatários. Ela continuará pendente.");
                continue;
            }

            boolean todosEnviados = emailService.enviarEmailEmLote(notificacao.getEmails(),
                    notificacao.getTitulo(), notificacao.getMensagem());

            if (todosEnviados) {
                try {
                    metodos.marcarComoLida(notificacao.getIdNotificacao());
                    System.out.println("Notificação " + notificacao.getIdNotificacao() + " marcada como lida.");
                } catch (SQLException e) {
                    System.out.println("Emails enviados, mas não foi possível marcar a notificação "
                            + notificacao.getIdNotificacao() + " como lida: " + e.getMessage());
                }
            } else {
                System.out.println("Notificação " + notificacao.getIdNotificacao()
                        + " com falha no envio. Ela continuará pendente.");
            }
        }
    }
}
