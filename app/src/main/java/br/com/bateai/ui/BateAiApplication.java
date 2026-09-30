package br.com.bateai.ui;

import android.app.Application;
import br.com.bateai.database.DatabaseHelper;
import br.com.bateai.repository.SQLitePontoRepository;
import br.com.bateai.repository.SQLiteUsuarioRepository;
import br.com.bateai.service.AuthService;
import br.com.bateai.service.JornadaService;
import br.com.bateai.service.PontoService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BateAiApplication extends Application {
    final ExecutorService bancoExecutor = Executors.newSingleThreadExecutor();
    AuthService auth;
    PontoService pontos;
    JornadaService jornadas;
    SessaoLocal sessao;

    @Override public void onCreate() {
        super.onCreate();
        DatabaseHelper helper = new DatabaseHelper(this);
        SQLiteUsuarioRepository usuarios = new SQLiteUsuarioRepository(helper);
        SQLitePontoRepository registros = new SQLitePontoRepository(helper);
        // O usuário pode mudar o fuso nas configurações sem encerrar o processo Android.
        Clock relogio = new Clock() {
            @Override public ZoneId getZone() { return ZoneId.systemDefault(); }
            @Override public Clock withZone(ZoneId zone) { return Clock.system(zone); }
            @Override public Instant instant() { return Instant.now(); }
        };
        auth = new AuthService(usuarios, relogio);
        pontos = new PontoService(usuarios, registros, relogio);
        jornadas = new JornadaService(registros, relogio);
        sessao = new SessaoLocal(this);
    }
}
