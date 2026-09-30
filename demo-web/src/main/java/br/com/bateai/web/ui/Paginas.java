package br.com.bateai.web.ui;

import br.com.bateai.model.Jornada;
import br.com.bateai.model.RegistroPonto;
import br.com.bateai.model.TipoPonto;
import br.com.bateai.model.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** HTML local com apresentação apenas; estados e totais vêm dos serviços compartilhados. */
public final class Paginas {
    private static final Locale PT = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT);
    private static final DateTimeFormatter DATA_EXTENSA = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss", PT);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss", PT);
    private final Clock relogio;

    public Paginas(Clock relogio) { this.relogio = relogio; }

    public String acesso(boolean cadastro, String csrf, String mensagem, boolean erro) {
        String titulo = cadastro ? "Crie sua conta" : "Bom ter você por aqui.";
        StringBuilder conteudo = new StringBuilder("<main class=\"auth-layout\"><aside class=\"brand-panel\">")
                .append(marca()).append("<div class=\"brand-story\"><span class=\"eyebrow light\">MAIS CLAREZA NO SEU DIA</span>")
                .append("<h1>Seu tempo<br>bem registrado.</h1><p>Uma batida de cada vez.<br>Acompanhe sua jornada com simplicidade.</p>")
                .append("<div class=\"clock-art\" aria-hidden=\"true\"><span></span></div></div>")
                .append("<p class=\"brand-foot\"><span class=\"dot\"></span> Funciona sem internet</p></aside>")
                .append("<section class=\"auth-content\"><div class=\"auth-card\"><span class=\"eyebrow\">SEU CONTROLE DE PONTO</span><h2>")
                .append(titulo).append("</h2><p class=\"muted\">")
                .append(cadastro ? "Comece a acompanhar sua jornada de trabalho." : "Entre na sua conta para acompanhar sua jornada.")
                .append("</p>").append(aviso(mensagem, erro)).append("<form method=\"post\" action=\"")
                .append(cadastro ? "/cadastro" : "/login").append("\" class=\"auth-form\">").append(csrf(csrf));
        if (cadastro) conteudo.append(campo("nome", "Seu nome", "text", "Como podemos chamar você?", "name", " required maxlength=\"120\""));
        conteudo.append(campo("email", "E-mail", "email", "voce@exemplo.com", "email", " required maxlength=\"254\""))
                .append(campo("senha", "Senha", "password", cadastro ? "Pelo menos 8 caracteres" : "Digite sua senha", cadastro ? "new-password" : "current-password", " required" + (cadastro ? " minlength=\"8\"" : "")));
        if (cadastro) conteudo.append(campo("confirmacao", "Confirme a senha", "password", "Digite sua senha novamente", "new-password", " required minlength=\"8\""));
        conteudo.append("<button class=\"button primary wide\" type=\"submit\">").append(cadastro ? "Criar conta" : "Entrar na minha conta")
                .append(" <span aria-hidden=\"true\">→</span></button></form><p class=\"auth-switch\">")
                .append(cadastro ? "Já tem uma conta? <a href=\"/login\">Entrar</a>" : "Ainda não tem conta? <a href=\"/cadastro\">Criar conta</a>")
                .append("</p><div class=\"local-note\"><span class=\"dot\"></span> Seus dados ficam neste computador.</div></div>")
                .append("<p class=\"auth-footer\">Bate Aí · Demonstração local</p></section></main>");
        return documento(cadastro ? "Cadastro" : "Entrar", conteudo.toString());
    }

    public String inicio(Usuario usuario, String csrf, Optional<Jornada> atual, List<Jornada> historico, List<TipoPonto> acoes, String mensagem, boolean erro) {
        Jornada jornada = atual.orElse(null);
        String status = jornada == null ? "Pronto para começar" : jornada.getStatus();
        long trabalhado = jornada == null ? 0 : jornada.getTrabalhadoMillis();
        long intervalo = jornada == null ? 0 : jornada.getIntervaloMillis();
        String data = (jornada == null ? LocalDate.now(relogio) : jornada.getData()).format(DATA);
        RegistroPonto ultima = null;
        if (!historico.isEmpty()) {
            List<RegistroPonto> registros = historico.get(0).getRegistros();
            if (!registros.isEmpty()) ultima = registros.get(registros.size() - 1);
        }
        StringBuilder conteudo = new StringBuilder(cabecalho("VISÃO GERAL", "Olá, " + usuario.getNome() + ".", "Vamos acompanhar sua jornada de hoje?"))
                .append(aviso(mensagem, erro))
                .append("<div class=\"section-line\"><h2>Sua jornada</h2><a class=\"refresh\" href=\"/\">↻ Atualizar</a></div>")
                .append("<section class=\"overview\"><div class=\"card work-card\"><div class=\"card-top\"><span class=\"eyebrow\">TEMPO TRABALHADO</span>")
                .append(status(status)).append("</div><div class=\"time-display\">").append(duracao(trabalhado)).append("</div>")
                .append("<p class=\"muted\">Jornada de ").append(data).append(jornada != null && jornada.isAberta() ? " · Total parcial" : "")
                .append("</p><div class=\"action-row\">");
        for (TipoPonto acao : acoes) {
            if (acao == TipoPonto.SAIDA) conteudo.append("<a class=\"button secondary\" href=\"/confirmar-saida\">Encerrar jornada</a>");
            else conteudo.append("<form method=\"post\" action=\"/ponto\">").append(csrf(csrf))
                    .append("<input type=\"hidden\" name=\"tipo\" value=\"").append(acao.name()).append("\"><button class=\"button primary\" type=\"submit\">")
                    .append(rotuloAcao(acao)).append("</button></form>");
        }
        if (acoes.isEmpty()) conteudo.append("<p class=\"finished\">Tudo certo por hoje. Sua jornada está salva.</p>");
        conteudo.append("</div><p class=\"snapshot\">Atualizado às ").append(hora(relogio.millis())).append(". Use Atualizar para rever os totais.</p></div>")
                .append("<div class=\"side-metrics\"><div class=\"card metric\"><span class=\"eyebrow\">TEMPO EM INTERVALO</span><strong>")
                .append(duracao(intervalo)).append("</strong><span class=\"muted\">Sua pausa também faz parte do dia.</span></div>")
                .append("<div class=\"card metric\"><span class=\"eyebrow\">ÚLTIMA BATIDA</span><strong class=\"last-time\">")
                .append(ultima == null ? "—" : hora(ultima.getTimestamp())).append("</strong><span class=\"muted\">")
                .append(ultima == null ? "Nenhum ponto registrado ainda." : escapar(ultima.getTipo().getDescricao()) + " · " + dataHora(ultima.getTimestamp()))
                .append("</span></div></div></section><section class=\"card timeline-card\"><div class=\"section-line\"><div><h2>Batidas da jornada</h2>")
                .append("<p class=\"muted\">Cada registro conta a história do seu dia.</p></div><a href=\"/historico\">Ver histórico →</a></div>");
        if (jornada == null) conteudo.append("<div class=\"empty\"><div class=\"empty-icon\" aria-hidden=\"true\">◷</div><h3>O primeiro passo é uma batida.</h3><p>Registre sua entrada para começar a acompanhar o dia.</p></div>");
        else conteudo.append(batidas(jornada));
        conteudo.append("</section><div class=\"help-note\"><span aria-hidden=\"true\">ⓘ</span><p>Uma jornada por dia, com um intervalo opcional. Os horários são registrados automaticamente.</p></div>");
        return aplicativo("Início", usuario, csrf, conteudo.toString());
    }

    public String historico(Usuario usuario, String csrf, List<Jornada> jornadas, String mensagem, boolean erro) {
        StringBuilder conteudo = new StringBuilder(cabecalho("SEUS REGISTROS", "Histórico de pontos", "Um registro organizado de cada jornada."))
                .append(aviso(mensagem, erro)).append("<div class=\"section-line\"><h2>").append(jornadas.size())
                .append(jornadas.size() == 1 ? " jornada registrada" : " jornadas registradas").append("</h2><a class=\"refresh\" href=\"/historico\">↻ Atualizar</a></div>");
        if (jornadas.isEmpty()) conteudo.append("<section class=\"card empty history-empty\"><div class=\"empty-icon\" aria-hidden=\"true\">◷</div><h2>Seu histórico começa aqui</h2><p>Ao registrar sua primeira entrada, sua jornada aparecerá nesta página.</p><a class=\"button primary\" href=\"/\">Ir para o início</a></section>");
        for (Jornada jornada : jornadas) {
            conteudo.append("<article class=\"card history-card\"><div class=\"section-line\"><div><span class=\"eyebrow\">JORNADA</span><h2>")
                    .append(jornada.getData().format(DATA)).append("</h2></div>").append(status(jornada.getStatus())).append("</div>")
                    .append("<div class=\"history-totals\"><div><span>Tempo trabalhado").append(jornada.isAberta() ? " · parcial" : "")
                    .append("</span><strong>").append(duracao(jornada.getTrabalhadoMillis())).append("</strong></div><div><span>Intervalo</span><strong>")
                    .append(duracao(jornada.getIntervaloMillis())).append("</strong></div></div>").append(batidas(jornada)).append("</article>");
        }
        conteudo.append("<p class=\"snapshot\">Jornadas noturnas aparecem na data da entrada. Horários no fuso deste computador.</p>");
        return aplicativo("Histórico", usuario, csrf, conteudo.toString());
    }

    public String confirmacao(Usuario usuario, String csrf) {
        String conteudo = cabecalho("FINALIZAR JORNADA", "Tudo pronto por hoje?", "Confira antes de registrar sua saída.")
                + "<section class=\"card confirm-card\"><div class=\"empty-icon\" aria-hidden=\"true\">✓</div><h2>Confirmar saída</h2>"
                + "<p>A saída será registrada com o horário atual deste computador. Depois disso, você não poderá iniciar outra jornada na mesma data da entrada.</p>"
                + "<div class=\"action-row\"><a class=\"button secondary\" href=\"/\">Voltar</a><form method=\"post\" action=\"/ponto\">" + csrf(csrf)
                + "<input type=\"hidden\" name=\"tipo\" value=\"SAIDA\"><input type=\"hidden\" name=\"confirmar\" value=\"sim\"><button class=\"button primary\" type=\"submit\">Confirmar saída</button></form></div></section>";
        return aplicativo("Confirmar saída", usuario, csrf, conteudo);
    }

    public String erro(String mensagem) {
        return documento("Não foi possível continuar", "<main class=\"error-page\">" + marca()
                + "<section class=\"card confirm-card\"><h1>Vamos tentar de novo?</h1><p>" + escapar(mensagem)
                + "</p><a class=\"button primary\" href=\"/\">Voltar ao início</a></section></main>");
    }

    private String aplicativo(String titulo, Usuario usuario, String csrf, String conteudo) {
        String lateral = "<aside class=\"sidebar\">" + marca() + "<span class=\"nav-label\">MEU ESPAÇO</span><nav aria-label=\"Menu principal\">"
                + "<a " + (titulo.equals("Início") ? "aria-current=\"page\" " : "") + "href=\"/\"><span aria-hidden=\"true\">◷</span> Início</a>"
                + "<a " + (titulo.equals("Histórico") ? "aria-current=\"page\" " : "") + "href=\"/historico\"><span aria-hidden=\"true\">≡</span> Histórico</a></nav>"
                + "<div class=\"sidebar-bottom\"><div class=\"profile\"><div class=\"avatar\">" + escapar(usuario.getNome().substring(0, 1).toUpperCase(PT))
                + "</div><div><strong>" + escapar(usuario.getNome()) + "</strong><small>Minha conta local</small></div></div>"
                + "<form method=\"post\" action=\"/logout\">" + csrf(csrf) + "<button class=\"logout\" type=\"submit\">Sair da conta <span aria-hidden=\"true\">↗</span></button></form>"
                + "<p class=\"sidebar-foot\"><span class=\"dot\"></span> Seus dados neste computador</p></div></aside>";
        return documento(titulo, "<div class=\"app-shell\">" + lateral + "<div class=\"workspace\"><header class=\"topbar\"><span>Seu tempo. Sua jornada.</span><span class=\"demo-badge\">Demonstração local</span></header><main class=\"content\">" + conteudo + "</main><footer class=\"workspace-footer\">Bate Aí · Feito para simplificar sua rotina</footer></div></div>");
    }

    private String cabecalho(String categoria, String titulo, String descricao) {
        return "<div class=\"page-heading\"><div><span class=\"eyebrow\">" + escapar(categoria) + "</span><h1>" + escapar(titulo)
                + "</h1><p class=\"muted\">" + escapar(descricao) + "</p></div><time class=\"today\">" + LocalDate.now(relogio).format(DATA_EXTENSA) + "</time></div>";
    }
    private String batidas(Jornada jornada) {
        StringBuilder html = new StringBuilder("<ol class=\"punch-list\">");
        for (RegistroPonto registro : jornada.getRegistros()) html.append("<li><span class=\"punch-dot\" aria-hidden=\"true\"></span><strong>")
                .append(escapar(registro.getTipo().getDescricao())).append("</strong><time>").append(dataHora(registro.getTimestamp())).append("</time></li>");
        return html.append("</ol>").toString();
    }
    private static String campo(String nome, String rotulo, String tipo, String placeholder, String autocomplete, String atributos) {
        return "<label for=\"" + nome + "\">" + rotulo + "</label><input id=\"" + nome + "\" name=\"" + nome + "\" type=\"" + tipo + "\" placeholder=\"" + placeholder + "\" autocomplete=\"" + autocomplete + "\"" + atributos + ">";
    }
    private static String aviso(String mensagem, boolean erro) { return mensagem.isEmpty() ? "" : "<div class=\"notice " + (erro ? "notice-error" : "notice-success") + "\" role=\"status\">" + escapar(mensagem) + "</div>"; }
    private static String status(String status) { return "<span class=\"status " + (status.equals("Trabalhando") ? "working" : status.equals("Em intervalo") ? "paused" : "") + "\"><span class=\"dot\"></span>" + escapar(status) + "</span>"; }
    private static String csrf(String csrf) { return "<input type=\"hidden\" name=\"csrf\" value=\"" + escapar(csrf) + "\">"; }
    private static String rotuloAcao(TipoPonto acao) {
        switch (acao) {
            case ENTRADA: return "Registrar entrada";
            case INICIO_INTERVALO: return "Iniciar intervalo";
            case FIM_INTERVALO: return "Voltar do intervalo";
            default: return acao.getDescricao();
        }
    }
    private String hora(long timestamp) { return Instant.ofEpochMilli(timestamp).atZone(relogio.getZone()).format(HORA); }
    private String dataHora(long timestamp) { return Instant.ofEpochMilli(timestamp).atZone(relogio.getZone()).format(DATA_HORA); }
    private static String duracao(long millis) { long segundos = millis / 1000; return String.format(PT, "%02d:%02d:%02d", segundos / 3600, segundos / 60 % 60, segundos % 60); }
    private static String marca() { return "<a class=\"brand\" href=\"/\" aria-label=\"Bate Aí, início\"><span class=\"brand-mark\" aria-hidden=\"true\">b.</span><span>Bate Aí<span class=\"brand-period\">.</span></span></a>"; }
    private static String documento(String titulo, String conteudo) { return "<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>" + escapar(titulo) + " · Bate Aí</title><link rel=\"stylesheet\" href=\"/assets/estilo.css\"></head><body>" + conteudo + "</body></html>"; }
    private static String escapar(String texto) { return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
}
