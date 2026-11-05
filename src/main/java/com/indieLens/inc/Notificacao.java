package com.indieLens.inc;

import java.util.ArrayList;

public class Notificacao {

    private final int idNotificacao;
    private final String titulo;
    private final String mensagem;
    private final ArrayList emails;

    public Notificacao(int idNotificacao, String titulo, String mensagem, ArrayList emails) {
        this.idNotificacao = idNotificacao;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.emails = emails;
    }

    public int getIdNotificacao() {
        return idNotificacao;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public ArrayList getEmails() {
        return emails;
    }
}
