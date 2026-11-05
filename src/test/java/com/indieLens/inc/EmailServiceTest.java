package com.indieLens.inc;

import com.indieLens.inc.emailService.EmailService;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Provider;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.URLName;
import java.lang.reflect.Field;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EmailServiceTest {

    private EmailService emailService;

    @BeforeEach
    void configurarTransporteSemRede() throws Exception {
        TransporteSimulado.mensagens = new ArrayList();
        TransporteSimulado.tentativas = new ArrayList();
        TransporteSimulado.emailComFalha = null;
        emailService = new EmailService("origem@example.com", "senha-simulada");
        Field campo = EmailService.class.getDeclaredField("session");
        campo.setAccessible(true);
        Session sessao = (Session) campo.get(emailService);
        sessao.setProvider(new Provider(Provider.Type.TRANSPORT, "smtp",
                TransporteSimulado.class.getName(), "Teste local sem rede", "1.0"));
    }

    @Test
    void envioPreparaRemetenteDestinatarioETermoUtf8() throws Exception {
        assertTrue(emailService.enviarEmail("destino@example.com", "Título: ação", "Olá, usuário!"));

        Message mensagem = (Message) TransporteSimulado.mensagens.get(0);
        assertEquals("origem@example.com", mensagem.getFrom()[0].toString());
        assertEquals("destino@example.com", mensagem.getAllRecipients()[0].toString());
        assertEquals(1, mensagem.getAllRecipients().length);
        assertEquals("Título: ação", mensagem.getSubject());
        assertEquals("Olá, usuário!", mensagem.getContent());
        assertTrue(mensagem.getContentType().toUpperCase().contains("UTF-8"));
    }

    @Test
    void falhaSmtpRetornaFalse() {
        TransporteSimulado.emailComFalha = "falha@example.com";

        assertFalse(emailService.enviarEmail("falha@example.com", "Título", "Mensagem"));
        assertTrue(TransporteSimulado.mensagens.isEmpty());
    }

    @Test
    void loteContinuaDepoisDeFalhaSemInformarSucessoTotal() {
        ArrayList destinatarios = new ArrayList();
        destinatarios.add("falha@example.com");
        destinatarios.add("sucesso@example.com");
        TransporteSimulado.emailComFalha = "falha@example.com";

        assertFalse(emailService.enviarEmailEmLote(destinatarios, "Título", "Mensagem"));
        assertEquals(2, TransporteSimulado.tentativas.size());
        assertEquals(1, TransporteSimulado.mensagens.size());
    }

    @Test
    void enderecosAusentesInvalidosOuComVariosDestinosNaoUsamTransporte() {
        assertFalse(emailService.enviarEmail(null, "Título", "Mensagem"));
        assertFalse(emailService.enviarEmail("   ", "Título", "Mensagem"));
        assertFalse(emailService.enviarEmail("sem-arroba", "Título", "Mensagem"));
        assertFalse(emailService.enviarEmail("a@example.com,b@example.com", "Título", "Mensagem"));
        assertTrue(TransporteSimulado.tentativas.isEmpty());
    }

    @Test
    void listaVaziaNaoEhSucesso() {
        assertFalse(emailService.enviarEmailEmLote(new ArrayList(), "Título", "Mensagem"));
        assertTrue(TransporteSimulado.tentativas.isEmpty());
    }

    public static class TransporteSimulado extends Transport {
        private static ArrayList mensagens;
        private static ArrayList tentativas;
        private static String emailComFalha;

        public TransporteSimulado(Session sessao, URLName endereco) {
            super(sessao, endereco);
        }

        @Override
        protected boolean protocolConnect(String host, int port, String user, String password) {
            return true;
        }

        @Override
        public void sendMessage(Message mensagem, Address[] enderecos) throws MessagingException {
            String email = enderecos[0].toString();
            tentativas.add(email);
            if (email.equals(emailComFalha)) {
                throw new MessagingException("Falha SMTP simulada");
            }
            mensagens.add(mensagem);
        }
    }
}
