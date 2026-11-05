package com.indieLens.inc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
public class ExecucaoTest {

    @TempDir
    Path pasta;

    @Test
    void singletonRetornaMesmaInstanciaEPermiteReiniciarAposFinalizar() throws Exception {
        String temporarioAnterior = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", pasta.toString());
        SingletonEvent evento = SingletonEvent.getInstance();

        try {
            assertSame(evento, SingletonEvent.getInstance());
            assertTrue(evento.iniciar());
            assertFalse(evento.iniciar());
            evento.finalizar();
            assertTrue(evento.iniciar());
        } finally {
            evento.finalizar();
            System.setProperty("java.io.tmpdir", temporarioAnterior);
        }

        assertTrue(Files.exists(pasta.resolve("indielens-javamail.lock")));
    }

    @Test
    void segundaJvmComOutroDiretorioEhIgnoradaEPodeExecutarAposLiberacao() throws Exception {
        Path primeiroDiretorio = Files.createDirectory(pasta.resolve("primeiro"));
        Path segundoDiretorio = Files.createDirectory(pasta.resolve("segundo"));
        Process primeiro = processo(primeiroDiretorio, SingletonEventProcess.class.getName(), "segurar").start();

        try {
            confirmarInicio(primeiro);
            String segundaSaida = executar(processo(segundoDiretorio, SingletonEventProcess.class.getName(), ""));
            assertEquals("OCUPADO", segundaSaida.trim());

            String saidaMain = executar(processo(segundoDiretorio, Main.class.getName(), ""));
            assertTrue(saidaMain.contains("já está em execução"));
            assertFalse(saidaMain.contains("Conexão com o banco"));

            primeiro.getOutputStream().write('\n');
            primeiro.getOutputStream().flush();
            assertTrue(primeiro.waitFor(10, TimeUnit.SECONDS));
            assertEquals(0, primeiro.exitValue());

            String terceiraSaida = executar(processo(segundoDiretorio, SingletonEventProcess.class.getName(), ""));
            assertEquals("INICIOU", terceiraSaida.trim());
        } finally {
            primeiro.destroyForcibly();
            primeiro.waitFor(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void travaEhLiberadaPeloSistemaSeJvmForEncerrada() throws Exception {
        Process primeiro = processo(pasta, SingletonEventProcess.class.getName(), "segurar").start();

        try {
            confirmarInicio(primeiro);
        } finally {
            primeiro.destroyForcibly();
            assertTrue(primeiro.waitFor(10, TimeUnit.SECONDS));
        }

        String saida = executar(processo(pasta, SingletonEventProcess.class.getName(), ""));
        assertEquals("INICIOU", saida.trim());
    }

    @Test
    void mainInformaErroDeConexaoFinalizaELiberaTrava() throws Exception {
        ProcessBuilder main = processo(pasta, Main.class.getName(), "");
        main.environment().put("DB_HOST", "127.0.0.1");
        main.environment().put("DB_PORT", "porta-invalida");
        main.environment().put("DB_NAME", "teste_sem_banco_real");
        main.environment().put("DB_USER", "usuario_simulado");
        main.environment().put("DB_SENHA", "senha_simulada");
        main.environment().put("EMAIL_FROM", "origem@example.com");
        main.environment().put("APP_PASSWORD", "senha_simulada");

        String saida = executar(main);

        assertTrue(saida.contains("Erro ao conectar ao banco de dados:"), saida);
        assertTrue(saida.contains("Processamento de emails finalizado."), saida);
        assertFalse(saida.contains("E-mail enviado com sucesso"));
        assertEquals("INICIOU", executar(processo(pasta, SingletonEventProcess.class.getName(), "")).trim());
    }

    private ProcessBuilder processo(Path diretorio, String classe, String argumento) {
        String java = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));

        return new ProcessBuilder(java, "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8",
                "-Dstderr.encoding=UTF-8", "-Djava.io.tmpdir=" + pasta,
                "-cp", classpath, classe, argumento).directory(diretorio.toFile()).redirectErrorStream(true);
    }

    private void confirmarInicio(Process processo) throws Exception {
        BufferedReader leitor = new BufferedReader(new InputStreamReader(processo.getInputStream(), StandardCharsets.UTF_8));
        assertEquals("INICIOU", leitor.readLine());
    }

    private String executar(ProcessBuilder comando) throws Exception {
        Process processo = comando.start();

        try {
            assertTrue(processo.waitFor(10, TimeUnit.SECONDS));
            String saida = new String(processo.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, processo.exitValue(), saida);
            return saida;
        } finally {
            processo.destroyForcibly();
            processo.waitFor(10, TimeUnit.SECONDS);
        }
    }
}
