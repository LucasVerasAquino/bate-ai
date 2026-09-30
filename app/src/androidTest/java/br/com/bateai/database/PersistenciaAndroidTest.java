package br.com.bateai.database;

import static org.junit.Assert.*;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import br.com.bateai.model.RegistroPonto;
import br.com.bateai.model.TipoPonto;
import br.com.bateai.model.Usuario;
import br.com.bateai.repository.SQLitePontoRepository;
import br.com.bateai.repository.SQLiteUsuarioRepository;
import br.com.bateai.repository.EmailDuplicadoException;
import br.com.bateai.repository.PersistenciaException;
import br.com.bateai.ui.SessaoLocal;
import br.com.bateai.service.PontoService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PersistenciaAndroidTest {
    private static final String BANCO = "teste-persistencia.db";
    private Context context;
    private DatabaseHelper helper;

    @Before public void preparar() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(BANCO);
        helper = new DatabaseHelper(context, BANCO);
        new SessaoLocal(context, "teste-sessao").encerrar();
    }

    @After public void limpar() {
        helper.close();
        context.deleteDatabase(BANCO);
        new SessaoLocal(context, "teste-sessao").encerrar();
    }

    @Test public void reabrirBanco_preservaUsuarioEHistoricoIsolado() {
        SQLiteUsuarioRepository usuarios = new SQLiteUsuarioRepository(helper);
        Usuario ana = usuarios.inserir(usuario("ana@teste.com"));
        Usuario bia = usuarios.inserir(usuario("bia@teste.com"));
        new SQLitePontoRepository(helper).inserir(ponto(ana.getId(), TipoPonto.ENTRADA));
        helper.close();
        helper = new DatabaseHelper(context, BANCO);
        assertEquals("Ana", new SQLiteUsuarioRepository(helper).buscarPorId(ana.getId()).get().getNome());
        assertArrayEquals(new byte[]{1, 2}, new SQLiteUsuarioRepository(helper).buscarPorEmail("ana@teste.com").get().getSenhaHash());
        assertEquals(1, new SQLitePontoRepository(helper).listar(ana.getId()).size());
        assertTrue(new SQLitePontoRepository(helper).listar(bia.getId()).isEmpty());
    }

    @Test public void falharTransacao_reverteBatida() {
        long id = new SQLiteUsuarioRepository(helper).inserir(usuario("ana@teste.com")).getId();
        SQLitePontoRepository pontos = new SQLitePontoRepository(helper);
        try {
            pontos.emTransacao(() -> {
                pontos.inserir(ponto(id, TipoPonto.ENTRADA));
                throw new IllegalStateException("Falha simulada");
            });
            fail("A falha precisa chegar ao chamador");
        } catch (IllegalStateException expected) {
            assertEquals("Falha simulada", expected.getMessage());
        }
        assertTrue(pontos.listar(id).isEmpty());
    }

    @Test public void constraints_impedemEmailDuplicadoPontoDuplicadoEUsuarioInexistente() {
        SQLiteUsuarioRepository usuarios = new SQLiteUsuarioRepository(helper);
        long id = usuarios.inserir(usuario("ana@teste.com")).getId();
        assertThrows(EmailDuplicadoException.class, () -> usuarios.inserir(usuario("ana@teste.com")));
        SQLitePontoRepository pontos = new SQLitePontoRepository(helper);
        pontos.inserir(ponto(id, TipoPonto.ENTRADA));
        assertThrows(PersistenciaException.class, () -> pontos.inserir(ponto(id, TipoPonto.ENTRADA)));
        assertThrows(PersistenciaException.class, () -> pontos.inserir(ponto(987654L, TipoPonto.ENTRADA)));
        assertEquals(1, pontos.listar(id).size());
    }

    @Test public void sessao_reabrirPreservaIdELogoutNaoApagaJornada() {
        long id = new SQLiteUsuarioRepository(helper).inserir(usuario("ana@teste.com")).getId();
        new SQLitePontoRepository(helper).inserir(ponto(id, TipoPonto.ENTRADA));
        SessaoLocal sessao = new SessaoLocal(context, "teste-sessao");
        sessao.iniciar(id);
        assertEquals(id, new SessaoLocal(context, "teste-sessao").usuarioId());
        sessao.encerrar();
        assertEquals(0L, new SessaoLocal(context, "teste-sessao").usuarioId());
        assertEquals(1, new SQLitePontoRepository(helper).listar(id).size());
    }

    @Test public void entradasSimultaneas_validamEGravamUmaUnicaBatida() throws Exception {
        SQLiteUsuarioRepository usuarios = new SQLiteUsuarioRepository(helper);
        long id = usuarios.inserir(usuario("ana@teste.com")).getId();
        SQLitePontoRepository pontos = new SQLitePontoRepository(helper);
        PontoService service = new PontoService(usuarios, pontos,
                Clock.fixed(Instant.parse("2026-09-30T08:00:00Z"), ZoneOffset.UTC));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            Future<Boolean> primeira = executor.submit(() -> { iniciar.await(); return service.registrar(id, TipoPonto.ENTRADA).isSucesso(); });
            Future<Boolean> segunda = executor.submit(() -> { iniciar.await(); return service.registrar(id, TipoPonto.ENTRADA).isSucesso(); });
            iniciar.countDown();
            assertNotEquals(primeira.get(), segunda.get());
            assertEquals(1, pontos.listar(id).size());
        } finally { executor.shutdownNow(); }
    }

    private Usuario usuario(String email) {
        return new Usuario(0L, "Ana", email, new byte[]{1, 2}, new byte[]{3, 4}, 1000L);
    }

    private RegistroPonto ponto(long id, TipoPonto tipo) {
        return new RegistroPonto(0L, id, tipo, 1000L, LocalDate.of(2026, 9, 30));
    }

}
