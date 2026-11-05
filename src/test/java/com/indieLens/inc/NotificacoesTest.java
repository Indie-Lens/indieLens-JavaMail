package com.indieLens.inc;

import com.indieLens.inc.Database.Conexao;
import com.indieLens.inc.Database.Methods;
import com.indieLens.inc.emailService.EmailService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NotificacoesTest {

    private JdbcDataSource fonte;
    private Connection banco;
    private Methods metodos;
    private EmailSimulado emailService;

    @BeforeEach
    void prepararBanco() throws SQLException {
        fonte = new JdbcDataSource();
        fonte.setURL("jdbc:h2:mem:notificacoes_" + System.nanoTime() + ";MODE=MySQL");
        banco = fonte.getConnection();

        try (Statement comando = banco.createStatement()) {
            comando.execute("CREATE TABLE usuario (id_usuario INT PRIMARY KEY, email VARCHAR(100))");
            comando.execute("CREATE TABLE notificacao (id_notificacao INT PRIMARY KEY, "
                    + "id_usuario INT NULL, id_jogo INT NULL, tipo VARCHAR(30) NOT NULL, titulo VARCHAR(100) NOT NULL, "
                    + "mensagem VARCHAR(255) NOT NULL, data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "lida TINYINT NOT NULL DEFAULT 0, data_leitura DATETIME(0))");
        }

        metodos = new Methods(fonte);
        emailService = new EmailSimulado();
    }

    @AfterEach
    void fecharBanco() throws SQLException {
        banco.close();
    }

    @Test
    void testaConexaoSemEnviarEmail() throws SQLException {
        new Conexao(fonte).checarConexao();
        assertTrue(emailService.destinatarios.isEmpty());
    }

    @Test
    void bancoVazioNaoEnviaEmails() throws SQLException {
        processar();
        assertTrue(emailService.destinatarios.isEmpty());
    }

    @Test
    void notificacaoLidaNaoEnviaNemAlteraData() throws SQLException {
        adicionarUsuario(1, "pessoa@example.com");
        adicionarNotificacao(1, 1, "INDIVIDUAL", 1);
        definirData(1, "2020-01-02 03:04:05");

        processar();

        assertTrue(emailService.destinatarios.isEmpty());
        assertEquals(Timestamp.valueOf("2020-01-02 03:04:05"), consultarData(1));
    }

    @Test
    void individualEnviaSomenteAoUsuarioRespectivoEGravaDataAtual() throws SQLException {
        adicionarUsuario(1, "primeiro@example.com");
        adicionarUsuario(2, "segundo@example.com");
        adicionarNotificacao(1, 2, "INDIVIDUAL", 0);
        long antes = System.currentTimeMillis() - 1000;

        processar();

        assertEquals(1, emailService.destinatarios.size());
        assertEquals("segundo@example.com", emailService.destinatarios.get(0));
        assertEquals("Título da notificação", emailService.titulo);
        assertEquals("Mensagem com acentuação: ação!", emailService.mensagem);
        assertEquals(1, consultarLida(1));
        assertNotNull(consultarData(1));
        assertTrue(consultarData(1).getTime() >= antes);
        assertTrue(consultarData(1).getTime() <= System.currentTimeMillis() + 1000);
    }

    @Test
    void globalEnviaATodosAntesDeMarcarNotificacao() throws SQLException {
        adicionarUsuario(1, "primeiro@example.com");
        adicionarUsuario(2, "segundo@example.com");
        adicionarUsuario(3, "terceiro@example.com");
        adicionarNotificacao(1, 2, "GLOBAL", 0);

        processar();

        assertEquals(3, emailService.destinatarios.size());
        assertTrue(emailService.destinatarios.contains("primeiro@example.com"));
        assertTrue(emailService.destinatarios.contains("segundo@example.com"));
        assertTrue(emailService.destinatarios.contains("terceiro@example.com"));
        assertEquals(1, consultarLida(1));
        assertNotNull(consultarData(1));
    }

    @Test
    void globalDaAtualizacaoDaBaseSemUsuarioOuJogoEnviaATodos() throws SQLException {
        adicionarUsuario(1, "primeiro@example.com");
        adicionarUsuario(2, "segundo@example.com");
        try (PreparedStatement comando = banco.prepareStatement(
                "INSERT INTO notificacao (id_notificacao, id_usuario, id_jogo, tipo, titulo, mensagem, lida) "
                        + "VALUES (1, NULL, NULL, 'GLOBAL', ?, ?, 0)")) {
            comando.setString(1, "nova base de dados!");
            comando.setString(2, "teste de atualização da base de dados foi um sucesso!");
            comando.executeUpdate();
        }

        processar();

        assertEquals(2, emailService.destinatarios.size());
        assertTrue(emailService.destinatarios.contains("primeiro@example.com"));
        assertTrue(emailService.destinatarios.contains("segundo@example.com"));
        assertEquals("nova base de dados!", emailService.titulo);
        assertEquals("teste de atualização da base de dados foi um sucesso!", emailService.mensagem);
        assertEquals(1, consultarLida(1));
        assertNotNull(consultarData(1));
    }

    @Test
    void falhaIndividualMantemDataAnteriorEContinuaOutrasNotificacoes() throws SQLException {
        adicionarUsuario(1, "falha@example.com");
        adicionarUsuario(2, "sucesso@example.com");
        adicionarNotificacao(1, 1, "INDIVIDUAL", 0);
        adicionarNotificacao(2, 2, "INDIVIDUAL", 0);
        definirData(1, "2020-01-02 03:04:05");
        emailService.emailComFalha = "falha@example.com";

        processar();

        assertEquals(2, emailService.destinatarios.size());
        assertEquals(0, consultarLida(1));
        assertEquals(Timestamp.valueOf("2020-01-02 03:04:05"), consultarData(1));
        assertEquals(1, consultarLida(2));
    }

    @Test
    void globalComFalhaContinuaDestinatariosEMantemPendente() throws SQLException {
        adicionarUsuario(1, "falha@example.com");
        adicionarUsuario(2, "sucesso@example.com");
        adicionarUsuario(3, "outro@example.com");
        adicionarNotificacao(1, 1, "GLOBAL", 0);
        emailService.emailComFalha = "falha@example.com";

        processar();

        assertEquals(3, emailService.destinatarios.size());
        assertEquals(0, consultarLida(1));
        assertNull(consultarData(1));
    }

    @Test
    void novaExecucaoIgnoraNotificacaoEnviadaComSucesso() throws SQLException {
        adicionarUsuario(1, "pessoa@example.com");
        adicionarNotificacao(1, 1, "INDIVIDUAL", 0);

        processar();
        Timestamp dataPrimeiroEnvio = consultarData(1);
        processar();

        assertEquals(1, emailService.destinatarios.size());
        assertEquals(dataPrimeiroEnvio, consultarData(1));
    }

    @Test
    void globalParcialEhTentadaNovamenteNaProximaExecucao() throws SQLException {
        adicionarUsuario(1, "falha@example.com");
        adicionarUsuario(2, "sucesso@example.com");
        adicionarNotificacao(1, 1, "GLOBAL", 0);
        emailService.emailComFalha = "falha@example.com";
        processar();
        emailService.emailComFalha = null;

        processar();

        assertEquals(4, emailService.destinatarios.size());
        assertEquals(1, consultarLida(1));
    }

    @Test
    void ausenciaDeUsuarioEGlobalSemUsuariosContinuamPendentes() throws SQLException {
        adicionarNotificacao(1, 99, "INDIVIDUAL", 0);
        adicionarNotificacao(2, 99, "GLOBAL", 0);

        processar();

        assertTrue(emailService.destinatarios.isEmpty());
        assertEquals(0, consultarLida(1));
        assertEquals(0, consultarLida(2));
        assertNull(consultarData(1));
        assertNull(consultarData(2));
    }

    @Test
    void emailsNulosBrancosEInvalidosNaoSaoMarcadosComoEnviados() throws SQLException {
        adicionarUsuario(1, null);
        adicionarUsuario(2, "   ");
        adicionarUsuario(3, "email-invalido");
        adicionarNotificacao(1, 1, "INDIVIDUAL", 0);
        adicionarNotificacao(2, 2, "INDIVIDUAL", 0);
        adicionarNotificacao(3, 3, "INDIVIDUAL", 0);
        EmailService servicoReal = new EmailService("origem@example.com", "senha-simulada");

        Main.processarNotificacoes(metodos.prepararNotificacoes(), metodos, servicoReal);

        assertEquals(0, consultarLida(1));
        assertEquals(0, consultarLida(2));
        assertEquals(0, consultarLida(3));
        assertNull(consultarData(1));
        assertNull(consultarData(2));
        assertNull(consultarData(3));
    }

    @Test
    void falhaNoUpdateMantemPendenteEPermiteProcessarProximaNotificacao() throws SQLException {
        adicionarUsuario(1, "pessoa@example.com");
        adicionarNotificacao(1, 1, "INDIVIDUAL", 0);
        adicionarNotificacao(2, 1, "INDIVIDUAL", 0);
        Methods bancoComFalha = new Methods(fonte) {
            @Override
            public void marcarComoLida(int idNotificacao) throws SQLException {
                if (idNotificacao == 1) {
                    throw new SQLException("Falha simulada no UPDATE");
                }
                super.marcarComoLida(idNotificacao);
            }
        };

        Main.processarNotificacoes(metodos.prepararNotificacoes(), bancoComFalha, emailService);

        assertEquals(2, emailService.destinatarios.size());
        assertEquals(0, consultarLida(1));
        assertNull(consultarData(1));
        assertEquals(1, consultarLida(2));
    }

    @Test
    void falhaNoSelectEhInformadaSemEnviarEmails() throws SQLException {
        try (Statement comando = banco.createStatement()) {
            comando.execute("DROP TABLE notificacao");
        }

        assertThrows(SQLException.class, () -> metodos.prepararNotificacoes());
        assertTrue(emailService.destinatarios.isEmpty());
    }

    @Test
    void atualizacaoNaoAlteraNotificacaoJaLidaOuOutroRegistro() throws SQLException {
        adicionarUsuario(1, "pessoa@example.com");
        adicionarNotificacao(1, 1, "INDIVIDUAL", 1);
        adicionarNotificacao(2, 1, "INDIVIDUAL", 0);
        definirData(1, "2020-01-02 03:04:05");

        assertThrows(SQLException.class, () -> metodos.marcarComoLida(1));
        assertThrows(SQLException.class, () -> metodos.marcarComoLida(99));

        assertEquals(Timestamp.valueOf("2020-01-02 03:04:05"), consultarData(1));
        assertEquals(0, consultarLida(2));
        assertNull(consultarData(2));
    }

    private void processar() throws SQLException {
        Main.processarNotificacoes(metodos.prepararNotificacoes(), metodos, emailService);
    }

    private void adicionarUsuario(int id, String email) throws SQLException {
        try (PreparedStatement comando = banco.prepareStatement("INSERT INTO usuario VALUES (?, ?)")) {
            comando.setInt(1, id);
            comando.setString(2, email);
            comando.executeUpdate();
        }
    }

    private void adicionarNotificacao(int id, int usuario, String tipo, int lida) throws SQLException {
        try (PreparedStatement comando = banco.prepareStatement(
                "INSERT INTO notificacao (id_notificacao, id_usuario, tipo, titulo, mensagem, lida) VALUES (?, ?, ?, ?, ?, ?)")) {
            comando.setInt(1, id);
            comando.setInt(2, usuario);
            comando.setString(3, tipo);
            comando.setString(4, "Título da notificação");
            comando.setString(5, "Mensagem com acentuação: ação!");
            comando.setInt(6, lida);
            comando.executeUpdate();
        }
    }

    private int consultarLida(int id) throws SQLException {
        try (PreparedStatement comando = banco.prepareStatement("SELECT lida FROM notificacao WHERE id_notificacao = ?")) {
            comando.setInt(1, id);
            try (ResultSet resultado = comando.executeQuery()) {
                assertTrue(resultado.next());
                return resultado.getInt("lida");
            }
        }
    }

    private Timestamp consultarData(int id) throws SQLException {
        try (PreparedStatement comando = banco.prepareStatement("SELECT data_leitura FROM notificacao WHERE id_notificacao = ?")) {
            comando.setInt(1, id);
            try (ResultSet resultado = comando.executeQuery()) {
                assertTrue(resultado.next());
                return resultado.getTimestamp("data_leitura");
            }
        }
    }

    private void definirData(int id, String data) throws SQLException {
        try (PreparedStatement comando = banco.prepareStatement("UPDATE notificacao SET data_leitura = ? WHERE id_notificacao = ?")) {
            comando.setTimestamp(1, Timestamp.valueOf(data));
            comando.setInt(2, id);
            comando.executeUpdate();
        }
    }

    private static class EmailSimulado extends EmailService {
        private final ArrayList destinatarios = new ArrayList();
        private String emailComFalha;
        private String titulo;
        private String mensagem;

        private EmailSimulado() {
            super("origem@example.com", "senha-simulada");
        }

        @Override
        public boolean enviarEmail(String destinatario, String titulo, String mensagem) {
            destinatarios.add(destinatario);
            this.titulo = titulo;
            this.mensagem = mensagem;
            return !destinatario.equals(emailComFalha);
        }
    }
}
