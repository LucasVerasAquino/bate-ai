package br.com.bateai.web;

import java.nio.file.Path;
import java.time.Clock;

public final class Main {
    private Main() { }
    public static void main(String[] argumentos) {
        Path arquivo = Path.of("data", "bateai-demo.db");
        int porta = 8080;
        for (int i = 0; i < argumentos.length; i++) {
            if (argumentos[i].equals("--db") && i + 1 < argumentos.length) arquivo = Path.of(argumentos[++i]);
            else if (argumentos[i].equals("--port") && i + 1 < argumentos.length) porta = Integer.parseInt(argumentos[++i]);
            else throw new IllegalArgumentException("Uso: demo-web [--db caminho] [--port número]");
        }
        ServidorWeb servidor = new ServidorWeb(arquivo, Clock.systemDefaultZone(), porta);
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::close, "fechar-bateai"));
        servidor.iniciar();
        System.out.println("Bate Aí disponível em http://127.0.0.1:" + servidor.getPorta());
        System.out.println("Banco local: " + arquivo.toAbsolutePath().normalize());
        System.out.println("Pressione Ctrl+C para encerrar.");
    }
}
