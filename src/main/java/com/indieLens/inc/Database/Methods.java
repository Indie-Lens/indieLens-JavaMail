package com.indieLens.inc.Database;

import com.indieLens.inc.Notificacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;

public class Methods {

    private final DataSource conexao;

    public Methods(DataSource conexao) {
        this.conexao = conexao;
    }

    public ArrayList prepararNotificacoes() throws SQLException {
        ArrayList notificacoes = new ArrayList();

        try (Connection banco = conexao.getConnection();
                PreparedStatement consulta = banco.prepareStatement("SELECT * FROM notificacao");
                ResultSet resultado = consulta.executeQuery()) {

            while (resultado.next()) {
                if (resultado.getInt("lida") != 0) {
                    continue;
                }

                int idNotificacao = resultado.getInt("id_notificacao");
                int idUsuario = resultado.getInt("id_usuario");
                String tipo = resultado.getString("tipo");
                String titulo = resultado.getString("titulo");
                String mensagem = resultado.getString("mensagem");
                ArrayList emails = buscarEmails(banco, idUsuario, tipo);

                notificacoes.add(new Notificacao(idNotificacao, titulo, mensagem, emails));
            }
        }

        return notificacoes;
    }

    private ArrayList buscarEmails(Connection banco, int idUsuario, String tipo) throws SQLException {
        ArrayList emails = new ArrayList();
        String sql = "SELECT email FROM usuario WHERE usuario.id_usuario = ?";

        if ("GLOBAL".equalsIgnoreCase(tipo)) {
            sql = "SELECT * FROM usuario";
        }

        try (PreparedStatement consulta = banco.prepareStatement(sql)) {
            if (!"GLOBAL".equalsIgnoreCase(tipo)) {
                consulta.setInt(1, idUsuario);
            }

            try (ResultSet resultado = consulta.executeQuery()) {
                while (resultado.next()) {
                    emails.add(resultado.getString("email"));
                }
            }
        }

        return emails;
    }

    public void marcarComoLida(int idNotificacao) throws SQLException {
        String sql = "UPDATE notificacao SET lida = 1, data_leitura = CURRENT_TIMESTAMP "
                + "WHERE id_notificacao = ? AND lida = 0";

        try (Connection banco = conexao.getConnection();
                PreparedStatement atualizacao = banco.prepareStatement(sql)) {
            atualizacao.setInt(1, idNotificacao);

            if (atualizacao.executeUpdate() != 1) {
                throw new SQLException("A notificação " + idNotificacao + " não foi atualizada.");
            }
        }
    }
}
