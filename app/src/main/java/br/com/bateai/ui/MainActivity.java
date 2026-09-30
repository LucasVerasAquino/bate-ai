package br.com.bateai.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import br.com.bateai.R;
import br.com.bateai.model.Jornada;
import br.com.bateai.model.RegistroPonto;
import br.com.bateai.model.Resultado;
import br.com.bateai.model.TipoPonto;
import br.com.bateai.model.Usuario;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MainActivity extends Activity {
    private static final Locale PT = new Locale("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss", PT);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss", PT);
    private BateAiApplication app;
    private FrameLayout conteudo;
    private TextView mensagem;
    private String tela = "login";
    private Usuario usuario;
    private boolean ocupado;

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        app = (BateAiApplication) getApplication();
        setContentView(R.layout.activity_main);
        conteudo = findViewById(R.id.conteudo);
        mensagem = findViewById(R.id.mensagem);
        configurarMargens();
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, this::voltar);
        }
        String anterior = estado == null ? "login" : estado.getString("tela", "login");
        executar(() -> {
            long id = app.sessao.usuarioId();
            Optional<Usuario> conta = id == 0L ? Optional.empty() : app.auth.buscarUsuario(id);
            if (id != 0L && !conta.isPresent()) app.sessao.encerrar();
            return conta;
        }, conta -> {
            if (conta.isPresent()) {
                usuario = conta.get();
                if ("historico".equals(anterior)) carregarHistorico();
                else carregarInicio("");
            } else {
                if ("cadastro".equals(anterior)) mostrarCadastro();
                else mostrarLogin();
                if (estado != null) {
                    preencherSeExiste(R.id.email, estado.getString("email", ""));
                    preencherSeExiste(R.id.nome, estado.getString("nome", ""));
                }
            }
        });
    }

    private void configurarMargens() {
        View raiz = findViewById(R.id.raiz);
        raiz.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets margens = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                view.setPadding(margens.left, margens.top, margens.right, margens.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        raiz.requestApplyInsets();
    }

    @Override protected void onSaveInstanceState(Bundle estado) {
        estado.putString("tela", tela);
        estado.putString("email", valorSeExiste(R.id.email));
        estado.putString("nome", valorSeExiste(R.id.nome));
        super.onSaveInstanceState(estado);
    }

    // API 33+ usa o callback nativo registrado em onCreate; este fallback atende API 26–32.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() { voltar(); }

    @Override protected void onResume() {
        super.onResume();
        if (usuario != null && app.sessao.usuarioId() != usuario.getId()) {
            usuario = null;
            mostrarLogin();
        }
    }

    private void voltar() {
        if (ocupado) return;
        if ("cadastro".equals(tela)) mostrarLogin();
        else if ("historico".equals(tela)) carregarInicio("");
        else finish();
    }

    private void trocarTela(int layout, String nome) {
        tela = nome;
        conteudo.removeAllViews();
        getLayoutInflater().inflate(layout, conteudo, true);
        mensagem.setVisibility(View.GONE);
        findViewById(R.id.rolagem).scrollTo(0, 0);
    }

    private void mostrarLogin() {
        trocarTela(R.layout.tela_login, "login");
        findViewById(R.id.entrar).setOnClickListener(v -> autenticar(false));
        findViewById(R.id.ir_cadastro).setOnClickListener(v -> mostrarCadastro());
        ((EditText) findViewById(R.id.senha)).setOnEditorActionListener((v, action, event) -> {
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE && !ocupado) {
                autenticar(false);
                return true;
            }
            return false;
        });
    }

    private void mostrarCadastro() {
        trocarTela(R.layout.tela_cadastro, "cadastro");
        findViewById(R.id.cadastrar).setOnClickListener(v -> autenticar(true));
        findViewById(R.id.voltar_login).setOnClickListener(v -> mostrarLogin());
    }

    private void autenticar(boolean cadastro) {
        String nome = valorSeExiste(R.id.nome);
        String email = valorSeExiste(R.id.email);
        String senha = valorSeExiste(R.id.senha);
        String confirmacao = valorSeExiste(R.id.confirmacao);
        ocultarTeclado();
        executar(() -> {
            Resultado<Usuario> resultado = cadastro ? app.auth.cadastrar(nome, email, senha, confirmacao)
                    : app.auth.login(email, senha);
            if (resultado.isSucesso() && !cadastro) app.sessao.iniciar(resultado.getValor().getId());
            return resultado;
        }, resultado -> {
            if (resultado.isSucesso()) {
                if (cadastro) {
                    mostrarLogin();
                    preencherSeExiste(R.id.email, resultado.getValor().getEmail());
                    mostrarMensagem("Conta criada. Entre para registrar sua primeira batida.", false);
                } else {
                    usuario = resultado.getValor();
                    carregarInicio("Bem-vindo de volta!");
                }
            } else mostrarMensagem(resultado.getMensagem(), true);
        });
    }

    private void carregarInicio(String aviso) {
        executar(() -> new Painel(app.jornadas.buscarAtual(usuario.getId()),
                app.jornadas.buscarHistorico(usuario.getId()), app.pontos.acoesDisponiveis(usuario.getId())), painel -> {
            trocarTela(R.layout.tela_inicio, "inicio");
            texto(R.id.data_hoje, LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", PT)));
            texto(R.id.saudacao, "Olá, " + usuario.getNome() + ".");
            Jornada atual = painel.atual.orElse(null);
            texto(R.id.status, atual == null ? "Pronto para começar" : atual.getStatus());
            texto(R.id.tempo_trabalhado, duracao(atual == null ? 0 : atual.getTrabalhadoMillis()));
            texto(R.id.tempo_intervalo, "Intervalo  ·  " + duracao(atual == null ? 0 : atual.getIntervaloMillis()));
            texto(R.id.jornada_data, atual == null ? "Nenhuma jornada iniciada hoje." : "Jornada de " + atual.getData().format(DATA));
            if (painel.historico.isEmpty()) texto(R.id.ultima_batida, "Sua primeira batida vai aparecer aqui.");
            else {
                List<RegistroPonto> registros = painel.historico.get(0).getRegistros();
                RegistroPonto ultima = registros.get(registros.size() - 1);
                texto(R.id.ultima_batida, "Última batida: " + ultima.getTipo().getDescricao() + "\n" + horarioCompleto(ultima.getTimestamp()));
            }
            texto(R.id.atualizado, "Atualizado às " + Instant.now().atZone(ZoneId.systemDefault()).format(HORA));
            LinearLayout acoes = findViewById(R.id.acoes);
            for (TipoPonto tipo : painel.acoes) adicionarAcao(acoes, tipo);
            texto(R.id.nota_jornada, painel.acoes.isEmpty()
                    ? "Jornada encerrada. Seu próximo dia começa com uma nova entrada."
                    : "Os horários são registrados automaticamente. Confira antes de encerrar a jornada.");
            findViewById(R.id.historico).setOnClickListener(v -> carregarHistorico());
            findViewById(R.id.atualizar).setOnClickListener(v -> carregarInicio("Totais atualizados."));
            findViewById(R.id.sair).setOnClickListener(v -> sair());
            if (!aviso.isEmpty()) mostrarMensagem(aviso, false);
        });
    }

    private void adicionarAcao(LinearLayout destino, TipoPonto tipo) {
        Button botao = new Button(this);
        LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        parametros.bottomMargin = dp(12);
        botao.setLayoutParams(parametros);
        botao.setMinHeight(dp(56));
        botao.setPadding(dp(14), dp(14), dp(14), dp(14));
        botao.setAllCaps(false);
        botao.setTextSize(16);
        botao.setStateListAnimator(null);
        botao.setBackgroundResource(tipo == TipoPonto.SAIDA ? R.drawable.botao_secundario : R.drawable.botao);
        botao.setBackgroundTintList(null);
        botao.setTextColor(getColor(tipo == TipoPonto.SAIDA ? R.color.azul : R.color.branco));
        botao.setText(rotuloAcao(tipo));
        botao.setTag(tipo.name());
        botao.setOnClickListener(v -> {
            if (tipo == TipoPonto.SAIDA) {
                new AlertDialog.Builder(this).setTitle("Encerrar a jornada?")
                        .setMessage("A saída será registrada agora. Não será possível iniciar outra jornada com a mesma data de entrada.")
                        .setNegativeButton("Continuar trabalhando", null)
                        .setPositiveButton("Registrar saída", (dialog, which) -> registrar(tipo)).show();
            } else registrar(tipo);
        });
        destino.addView(botao);
    }

    private String rotuloAcao(TipoPonto tipo) {
        switch (tipo) {
            case ENTRADA: return "Registrar entrada";
            case INICIO_INTERVALO: return "Iniciar intervalo";
            case FIM_INTERVALO: return "Voltar do intervalo";
            case SAIDA: return "Encerrar jornada";
            default: throw new IllegalArgumentException("Tipo de ponto desconhecido");
        }
    }

    private void registrar(TipoPonto tipo) {
        if (ocupado) return;
        executar(() -> app.pontos.registrar(usuario.getId(), tipo), resultado -> {
            if (resultado.isSucesso()) {
                RegistroPonto salvo = resultado.getValor();
                carregarInicio(salvo.getTipo().getDescricao() + " registrada em " + horarioCompleto(salvo.getTimestamp()) + ".");
            } else mostrarMensagem(resultado.getMensagem(), true);
        });
    }

    private void carregarHistorico() {
        executar(() -> app.jornadas.buscarHistorico(usuario.getId()), jornadas -> {
            trocarTela(R.layout.tela_historico, "historico");
            LinearLayout lista = findViewById(R.id.lista_jornadas);
            if (jornadas.isEmpty()) {
                TextView vazio = new TextView(this);
                vazio.setText("Seu histórico está começando.\nRegistre uma entrada no início para acompanhar sua primeira jornada.");
                vazio.setTextColor(getColor(R.color.texto_secundario));
                vazio.setTextSize(16);
                vazio.setPadding(dp(20), dp(22), dp(20), dp(22));
                vazio.setBackgroundResource(R.drawable.card);
                LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(-1, -2);
                parametros.bottomMargin = dp(20);
                lista.addView(vazio, parametros);
            }
            for (Jornada jornada : jornadas) adicionarJornada(lista, jornada);
            findViewById(R.id.atualizar).setOnClickListener(v -> carregarHistorico());
            findViewById(R.id.voltar_inicio).setOnClickListener(v -> carregarInicio(""));
        });
    }

    private void adicionarJornada(LinearLayout lista, Jornada jornada) {
        View card = getLayoutInflater().inflate(R.layout.item_jornada, lista, false);
        ((TextView) card.findViewById(R.id.data_jornada)).setText(jornada.getData().format(DATA));
        ((TextView) card.findViewById(R.id.status_jornada)).setText(jornada.isAberta()
                ? getString(R.string.status_parcial, jornada.getStatus()) : jornada.getStatus());
        ((TextView) card.findViewById(R.id.resumo_jornada)).setText(getString(R.string.resumo_horas,
                duracao(jornada.getTrabalhadoMillis()), duracao(jornada.getIntervaloMillis())));
        LinearLayout batidas = card.findViewById(R.id.batidas_jornada);
        for (RegistroPonto ponto : jornada.getRegistros()) {
            TextView linha = new TextView(this);
            linha.setText(getString(R.string.linha_batida, ponto.getTipo().getDescricao(), horarioCompleto(ponto.getTimestamp())));
            linha.setTextSize(14);
            linha.setTextColor(getColor(R.color.texto_secundario));
            linha.setPadding(0, dp(8), 0, dp(8));
            batidas.addView(linha);
        }
        lista.addView(card);
    }

    private void sair() {
        executar(() -> { app.sessao.encerrar(); return true; }, ok -> {
            usuario = null;
            mostrarLogin();
            mostrarMensagem("Você saiu da conta. Sua jornada e seus registros estão salvos.", false);
        });
    }

    private <T> void executar(Supplier<T> tarefa, Consumer<T> concluido) {
        ocupado = true;
        alterarHabilitado(conteudo, false);
        findViewById(R.id.progresso).setVisibility(View.VISIBLE);
        app.bancoExecutor.execute(() -> {
            try {
                T resultado = tarefa.get();
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    liberarInterface();
                    concluido.accept(resultado);
                });
            } catch (RuntimeException ex) {
                Log.e("BateAi", "Operação local não concluída: " + ex.getClass().getSimpleName());
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    liberarInterface();
                    if (conteudo.getChildCount() == 0) mostrarLogin();
                    mostrarMensagem("Não foi possível concluir a operação. Seus registros anteriores estão salvos. Tente novamente.", true);
                });
            }
        });
    }

    private void liberarInterface() {
        ocupado = false;
        alterarHabilitado(conteudo, true);
        findViewById(R.id.progresso).setVisibility(View.INVISIBLE);
    }

    private void alterarHabilitado(View view, boolean habilitado) {
        view.setEnabled(habilitado);
        if (view instanceof ViewGroup) {
            ViewGroup grupo = (ViewGroup) view;
            for (int i = 0; i < grupo.getChildCount(); i++) alterarHabilitado(grupo.getChildAt(i), habilitado);
        }
    }

    private void mostrarMensagem(String valor, boolean erro) {
        mensagem.setText(valor);
        mensagem.setTextColor(erro ? 0xFF9C2E28 : getColor(R.color.verde));
        mensagem.setVisibility(View.VISIBLE);
        findViewById(R.id.rolagem).scrollTo(0, 0);
    }

    private void ocultarTeclado() {
        View foco = getCurrentFocus();
        if (foco != null) {
            InputMethodManager teclado = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            teclado.hideSoftInputFromWindow(foco.getWindowToken(), 0);
            foco.clearFocus();
        }
    }

    private String valorSeExiste(int id) {
        EditText campo = findViewById(id);
        return campo == null ? "" : campo.getText().toString();
    }

    private void preencherSeExiste(int id, String valor) {
        EditText campo = findViewById(id);
        if (campo != null) campo.setText(valor);
    }

    private void texto(int id, String valor) { ((TextView) findViewById(id)).setText(valor); }
    private int dp(int valor) { return Math.round(valor * getResources().getDisplayMetrics().density); }
    private String horarioCompleto(long timestamp) { return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DATA_HORA); }

    private String duracao(long millis) {
        long segundos = Math.max(0L, millis) / 1000L;
        return String.format(PT, "%02d:%02d:%02d", segundos / 3600L, segundos / 60L % 60L, segundos % 60L);
    }

    private static final class Painel {
        final Optional<Jornada> atual;
        final List<Jornada> historico;
        final List<TipoPonto> acoes;
        Painel(Optional<Jornada> atual, List<Jornada> historico, List<TipoPonto> acoes) {
            this.atual = atual;
            this.historico = historico;
            this.acoes = acoes;
        }
    }
}
