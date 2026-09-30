package br.com.bateai.web;

import br.com.bateai.model.RegistroPonto;
import br.com.bateai.model.TipoPonto;
import br.com.bateai.model.Usuario;
import br.com.bateai.repository.EmailDuplicadoException;
import br.com.bateai.repository.PersistenciaException;
import br.com.bateai.web.repository.SqliteRepository;
import br.com.bateai.service.PontoService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

public class SqliteRepositoryTest {
    @Rule public TemporaryFolder pasta = new TemporaryFolder();
    private final Clock relogio = Clock.fixed(Instant.parse("2026-09-30T11:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    @Test public void reabrirBanco_preservaUsuarioPontosEIsolaContas() throws Exception {
        Path arquivo = pasta.getRoot().toPath().resolve("pontos.db");
        long usuarioId;
        try (SqliteRepository banco = new SqliteRepository(arquivo)) {
            Usuario usuario = banco.inserir(usuario("ana@example.com"));
            usuarioId = usuario.getId();
            banco.inserir(new RegistroPonto(0, usuarioId, TipoPonto.ENTRADA, relogio.millis(), LocalDate.of(2026, 9, 30)));
            Usuario outro = banco.inserir(usuario("bia@example.com"));
            assertTrue(banco.listar(outro.getId()).isEmpty());
        }
        try (SqliteRepository banco = new SqliteRepository(arquivo)) {
            assertEquals("Ana", banco.buscarPorId(usuarioId).get().getNome());
            assertArrayEquals(new byte[]{1, 2, 3}, banco.buscarPorEmail("ana@example.com").get().getSenhaHash());
            assertEquals(1, banco.listar(usuarioId).size());
            assertEquals(TipoPonto.ENTRADA, banco.listar(usuarioId).get(0).getTipo());
        }
    }

    @Test public void inserirEmailDuplicado_informaErroEspecifico() {
        try (SqliteRepository banco = novoBanco()) {
            banco.inserir(usuario("ana@example.com"));
            assertThrows(EmailDuplicadoException.class, () -> banco.inserir(usuario("ana@example.com")));
        }
    }

    @Test public void inserirPonto_semUsuario_rejeitaChaveEstrangeira() {
        try (SqliteRepository banco = novoBanco()) {
            assertThrows(PersistenciaException.class, () -> banco.inserir(new RegistroPonto(0, 999, TipoPonto.ENTRADA, 1, LocalDate.of(2026, 9, 30))));
        }
    }

    @Test public void transacao_quandoFalha_desfazRegistro() {
        try (SqliteRepository banco = novoBanco()) {
            long id = banco.inserir(usuario("ana@example.com")).getId();
            assertThrows(IllegalStateException.class, () -> banco.emTransacao(() -> {
                banco.inserir(new RegistroPonto(0, id, TipoPonto.ENTRADA, 1, LocalDate.of(2026, 9, 30)));
                throw new IllegalStateException("falha simulada");
            }));
            assertTrue(banco.listar(id).isEmpty());
        }
    }

    @Test public void entradasConcorrentes_gravamApenasUmaBatida() throws Exception {
        try (SqliteRepository banco = novoBanco()) {
            long id = banco.inserir(usuario("ana@example.com")).getId();
            PontoService pontos = new PontoService(banco, banco, relogio);
            ExecutorService executor = Executors.newFixedThreadPool(8);
            try {
                List<Callable<Boolean>> chamadas = new ArrayList<>();
                for (int i = 0; i < 8; i++) chamadas.add(() -> pontos.registrar(id, TipoPonto.ENTRADA).isSucesso());
                int sucessos = 0;
                for (Future<Boolean> resultado : executor.invokeAll(chamadas)) if (resultado.get()) sucessos++;
                assertEquals(1, sucessos);
                assertEquals(1, banco.listar(id).size());
            } finally { executor.shutdownNow(); }
        }
    }

    @Test public void abrirVersaoFutura_recusaSemApagarDados() throws Exception {
        Path arquivo = pasta.getRoot().toPath().resolve("futuro.db");
        try (SqliteRepository banco = new SqliteRepository(arquivo)) { banco.inserir(usuario("ana@example.com")); }
        try (Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + arquivo); Statement comando = conexao.createStatement()) {
            comando.execute("PRAGMA user_version=2");
        }
        assertThrows(PersistenciaException.class, () -> new SqliteRepository(arquivo));
        try (Connection conexao = DriverManager.getConnection("jdbc:sqlite:" + arquivo); Statement comando = conexao.createStatement();
             java.sql.ResultSet resultado = comando.executeQuery("SELECT count(*) FROM usuarios")) {
            assertTrue(resultado.next()); assertEquals(1, resultado.getInt(1));
        }
    }

    @Test public void intervaloESaidaConcorrentes_confirmamSomenteUmaTransicao() throws Exception {
        try (SqliteRepository banco = novoBanco()) {
            long id = banco.inserir(usuario("ana@example.com")).getId();
            PontoService pontos = new PontoService(banco, banco, relogio);
            assertTrue(pontos.registrar(id, TipoPonto.ENTRADA).isSucesso());
            ExecutorService executor = Executors.newFixedThreadPool(2);
            java.util.concurrent.CountDownLatch iniciar = new java.util.concurrent.CountDownLatch(1);
            try {
                Future<Boolean> pausa = executor.submit(() -> { iniciar.await(); return pontos.registrar(id, TipoPonto.INICIO_INTERVALO).isSucesso(); });
                Future<Boolean> saida = executor.submit(() -> { iniciar.await(); return pontos.registrar(id, TipoPonto.SAIDA).isSucesso(); });
                iniciar.countDown();
                assertNotEquals(pausa.get(), saida.get());
                assertEquals(2, banco.listar(id).size());
            } finally { executor.shutdownNow(); }
        }
    }

    private SqliteRepository novoBanco() {
        return new SqliteRepository(pasta.getRoot().toPath().resolve("pontos.db"));
    }
    private Usuario usuario(String email) { return new Usuario(0, "Ana", email, new byte[]{1, 2, 3}, new byte[]{4, 5, 6}, relogio.millis()); }
}
