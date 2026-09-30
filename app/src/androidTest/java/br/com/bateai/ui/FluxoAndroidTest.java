package br.com.bateai.ui;

import static org.junit.Assert.*;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import br.com.bateai.R;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FluxoAndroidTest {
    private Instrumentation instrumentacao;
    private SessaoLocal sessao;
    private MainActivity activity;

    @Before public void preparar() {
        instrumentacao = InstrumentationRegistry.getInstrumentation();
        sessao = new SessaoLocal(instrumentacao.getTargetContext());
        sessao.encerrar();
    }

    @After public void limpar() {
        if (activity != null) instrumentacao.runOnMainSync(() -> activity.finish());
        sessao.encerrar();
    }

    @Test public void cadastrarBaterPontoRecriarEEntrarNovamente_preservaJornada() throws Exception {
        abrir();
        esperarTela(R.id.ir_cadastro);
        clicar(R.id.ir_cadastro);
        String email = "fluxo" + System.nanoTime() + "@teste.local";
        preencher(R.id.nome, "Pessoa teste");
        preencher(R.id.email, email);
        preencher(R.id.senha, "senha-segura-123");
        preencher(R.id.confirmacao, "senha-segura-123");
        clicar(R.id.cadastrar);
        esperarTela(R.id.entrar);
        assertEquals(0L, sessao.usuarioId());
        assertEquals(email, texto(R.id.email));
        preencher(R.id.senha, "senha-segura-123");
        clicar(R.id.entrar);
        esperarTela(R.id.historico);
        long usuarioId = sessao.usuarioId();
        assertTrue(usuarioId > 0);
        clicarPonto("ENTRADA");
        esperarTexto(R.id.status, "Trabalhando");

        Instrumentation.ActivityMonitor monitor = instrumentacao.addMonitor(MainActivity.class.getName(), null, false);
        instrumentacao.runOnMainSync(() -> activity.recreate());
        activity = (MainActivity) instrumentacao.waitForMonitorWithTimeout(monitor, 15000L);
        instrumentacao.removeMonitor(monitor);
        assertNotNull("A Activity deve ser recriada", activity);
        esperarTexto(R.id.status, "Trabalhando");
        assertEquals(usuarioId, sessao.usuarioId());
        clicar(R.id.historico);
        esperarTela(R.id.lista_jornadas);
        assertTrue(texto(R.id.resumo_jornada).contains("Trabalhado:"));
        clicar(R.id.voltar_inicio);
        esperarTela(R.id.sair);
        clicar(R.id.sair);
        esperarTela(R.id.entrar);
        assertEquals(0L, sessao.usuarioId());
        preencher(R.id.email, email);
        preencher(R.id.senha, "senha-segura-123");
        clicar(R.id.entrar);
        esperarTexto(R.id.status, "Trabalhando");
        assertEquals(usuarioId, sessao.usuarioId());
        clicarPonto("INICIO_INTERVALO");
        esperarTexto(R.id.status, "Em intervalo");
        clicarPonto("FIM_INTERVALO");
        esperarTexto(R.id.status, "Trabalhando");
        clicarPonto("SAIDA");
        instrumentacao.waitForIdleSync();
        assertEquals("Trabalhando", texto(R.id.status));
        android.accessibilityservice.AccessibilityServiceInfo acesso = instrumentacao.getUiAutomation().getServiceInfo();
        acesso.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        instrumentacao.getUiAutomation().setServiceInfo(acesso);
        instrumentacao.getUiAutomation().waitForIdle(250L, 5000L);
        AccessibilityNodeInfo janela = instrumentacao.getUiAutomation().getRootInActiveWindow();
        assertNotNull(janela);
        boolean confirmou = false;
        for (AccessibilityNodeInfo no : janela.findAccessibilityNodeInfosByViewId("android:id/button1")) {
            if (no.isEnabled() && no.isClickable()) {
                confirmou = no.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
        }
        assertTrue("A saída precisa ser confirmada no diálogo", confirmou);
        esperarTexto(R.id.status, "Encerrada");
        instrumentacao.runOnMainSync(() -> assertEquals(0,
                ((android.widget.LinearLayout) activity.findViewById(R.id.acoes)).getChildCount()));
    }

    @Test public void abrirComSessaoDeUsuarioInexistente_retornaAoLogin() {
        sessao.iniciar(Long.MAX_VALUE);
        abrir();
        esperarTela(R.id.entrar);
        assertEquals(0L, sessao.usuarioId());
    }

    @Test public void relancarAplicativo_reutilizaActivitySemManterOutraContaAberta() {
        abrir();
        esperarTela(R.id.ir_cadastro);
        Intent intent = new Intent(instrumentacao.getTargetContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        instrumentacao.getTargetContext().startActivity(intent);
        instrumentacao.waitForIdleSync();
        SystemClock.sleep(500L);
        instrumentacao.runOnMainSync(() -> assertEquals(Stage.RESUMED,
                ActivityLifecycleMonitorRegistry.getInstance().getLifecycleStageOf(activity)));
    }

    @Test public void mudarFusoComProcessoAberto_novaEntradaUsaDataLocalAtual() {
        BateAiApplication app = (BateAiApplication) instrumentacao.getTargetContext().getApplicationContext();
        java.util.TimeZone anterior = java.util.TimeZone.getDefault();
        try {
            for (String zona : new String[]{"Pacific/Kiritimati", "Pacific/Honolulu"}) {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zona));
                br.com.bateai.model.Usuario conta = app.auth.cadastrar("Fuso teste", "fuso" + System.nanoTime() + "@teste.local",
                        "senha-segura-123", "senha-segura-123").getValor();
                br.com.bateai.model.RegistroPonto registro = app.pontos.registrar(conta.getId(), br.com.bateai.model.TipoPonto.ENTRADA).getValor();
                assertEquals(java.time.Instant.ofEpochMilli(registro.getTimestamp()).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                        registro.getDataJornada());
            }
        } finally { java.util.TimeZone.setDefault(anterior); }
    }

    private void abrir() {
        Intent intent = new Intent(instrumentacao.getTargetContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity = (MainActivity) instrumentacao.startActivitySync(intent);
    }

    private void clicar(int id) {
        instrumentacao.runOnMainSync(() -> activity.findViewById(id).performClick());
    }

    private void clicarPonto(String tipo) {
        instrumentacao.runOnMainSync(() -> {
            View acao = activity.findViewById(R.id.acoes).findViewWithTag(tipo);
            assertNotNull("Ação disponível: " + tipo, acao);
            acao.performClick();
        });
    }

    private void preencher(int id, String valor) {
        instrumentacao.runOnMainSync(() -> ((EditText) activity.findViewById(id)).setText(valor));
    }

    private String texto(int id) {
        AtomicReference<String> valor = new AtomicReference<>();
        instrumentacao.runOnMainSync(() -> valor.set(((TextView) activity.findViewById(id)).getText().toString()));
        return valor.get();
    }

    private void esperarTela(int id) { esperar(id, null); }
    private void esperarTexto(int id, String texto) { esperar(id, texto); }

    private void esperar(int id, String texto) {
        long limite = SystemClock.uptimeMillis() + 25000L;
        while (SystemClock.uptimeMillis() < limite) {
            AtomicBoolean pronta = new AtomicBoolean();
            instrumentacao.runOnMainSync(() -> {
                View view = activity.findViewById(id);
                boolean terminou = activity.findViewById(R.id.progresso).getVisibility() != View.VISIBLE;
                pronta.set(terminou && view != null && view.isEnabled()
                        && (texto == null || (view instanceof TextView && texto.equals(((TextView) view).getText().toString()))));
            });
            if (pronta.get()) return;
            SystemClock.sleep(50L);
        }
        fail("A tela esperada não apareceu: " + id + " / " + texto);
    }
}
