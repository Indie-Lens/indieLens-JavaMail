package com.indieLens.inc; 

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException; 
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption; 

public class SingletonEvent {
    private static final SingletonEvent instancia = new SingletonEvent(); // Cria uma única instância nesta JVM.
    private FileChannel canal; // Guarda o arquivo aberto enquanto o serviço está executando.
    private FileLock trava; // Guarda a trava que impede outra execução de iniciar.

    private SingletonEvent() { // Impede que outras classes criem instâncias com new.
    } 

    public static SingletonEvent getInstance() { // Permite obter a única instância desta classe.
        return instancia; // Retorna sempre a mesma instância.
    } 

    public synchronized boolean iniciar() throws IOException { // Evita que duas threads iniciem ao mesmo tempo.
    	
        if (trava != null) { // Verifica se esta instância já iniciou o serviço.
            return false; // Informa que o serviço já está executando nesta JVM.
        } 
        
        Path arquivo = Paths.get(System.getProperty("java.io.tmpdir"), "indielens-javamail.lock"); // Usa o mesmo arquivo temporário para as execuções do mesmo usuário, mesmo em diretórios diferentes.
        canal = FileChannel.open(arquivo, StandardOpenOption.CREATE, StandardOpenOption.WRITE); // Abre ou cria o arquivo sem apagar uma trava existente.
        
        try { // Inicia a tentativa de obter a trava do sistema operacional.
            trava = canal.tryLock(); // Tenta obter a trava imediatamente, sem aguardar outra execução terminar.
            
        } catch (OverlappingFileLockException e) { // Trata outra trava já adquirida dentro da mesma JVM.
            canal.close(); // Fecha o arquivo que esta tentativa abriu.
            canal = null; // Remove a referência ao arquivo fechado.
            return false; // Informa que outra execução já possui a trava.
            
        } catch (IOException e) { // Trata falhas ao obter a trava no arquivo.
            canal.close(); // Fecha o arquivo para não deixar recursos abertos.
            canal = null; // Remove a referência ao arquivo fechado.
            throw e; // Devolve o erro para a Main informar o problema e encerrar.
        }
        
        if (trava == null) { // Verifica se outro processo Java já possui a trava.
            canal.close(); // Fecha o arquivo porque esta execução não poderá continuar.
            canal = null; // Remove a referência ao arquivo fechado.
            return false; // Informa que o cron deve ignorar esta execução.
        } 
        return true; // Autoriza o serviço a continuar porque esta execução adquiriu a trava.
    } 

    public synchronized void finalizar() throws IOException { 
        try { 
            if (trava != null) {
                trava.release(); // Permite que uma futura execução do cron adquira a trava.
            } 
        } finally {
            trava = null; // Remove a referência à trava anterior.
            if (canal != null) { // Confere se existe um arquivo aberto.
                try { // Garante a limpeza da referência ao arquivo.
                    canal.close(); // Fecha o arquivo; o sistema operacional também libera a trava se a JVM terminar.
                } finally { // Executa a limpeza da referência mesmo se o fechamento falhar.
                    canal = null; // Permite que esta instância tente iniciar novamente.
                }
            } 
        } 
    } 
} 
