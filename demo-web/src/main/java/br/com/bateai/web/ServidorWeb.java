package br.com.bateai.web;

import br.com.bateai.model.Resultado;
import br.com.bateai.model.TipoPonto;
import br.com.bateai.model.Usuario;
import br.com.bateai.model.RegistroPonto;
import br.com.bateai.service.AuthService;
import br.com.bateai.service.JornadaService;
import br.com.bateai.service.PontoService;
import br.com.bateai.web.repository.SqliteRepository;
import br.com.bateai.web.ui.Paginas;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

public final class ServidorWeb implements AutoCloseable {
    private static final String COOKIE = "bateai_sessao";
    private static final int LIMITE_CORPO = 16_384;
    private final SqliteRepository banco;
    private final HttpServer servidor;
    private final AuthService auth;
    private final PontoService pontos;
    private final JornadaService jornadas;
    private final Paginas paginas;
    private final SecureRandom aleatorio = new SecureRandom();
    private final Map<String, Sessao> sessoes = new HashMap<>();
    private final byte[] css;

    public ServidorWeb(Path arquivo, Clock relogio, int porta) {
        banco = new SqliteRepository(arquivo);
        auth = new AuthService(banco, relogio);
        pontos = new PontoService(banco, banco, relogio);
        jornadas = new JornadaService(banco, relogio);
        paginas = new Paginas(relogio);
        try (InputStream recurso = ServidorWeb.class.getResourceAsStream("/estilo.css")) {
            if (recurso == null) throw new IOException("Recurso de estilos indisponível.");
            css = recurso.readAllBytes();
            servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
            servidor.createContext("/", this::atender);
            // O executor nativo atende sequencialmente, suficiente para a demonstração local.
            servidor.setExecutor(null);
        } catch (IOException erro) {
            banco.close(); throw new IllegalStateException("Não foi possível iniciar a demonstração local.", erro);
        }
    }

    public void iniciar() { servidor.start(); }
    public int getPorta() { return servidor.getAddress().getPort(); }

    private void atender(HttpExchange requisicao) throws IOException {
        try {
            cabecalhos(requisicao);
            String host = requisicao.getRequestHeaders().getFirst("Host");
            if (!("127.0.0.1:" + getPorta()).equals(host) && !("localhost:" + getPorta()).equals(host)) {
                erro(requisicao, 403, "Acesse a demonstração pelo endereço local informado."); return;
            }
            String metodo = requisicao.getRequestMethod();
            String caminho = requisicao.getRequestURI().getPath();
            if (!metodo.equals("GET") && !metodo.equals("POST")) {
                requisicao.getResponseHeaders().set("Allow", "GET, POST"); erro(requisicao, 405, "Método não permitido."); return;
            }
            if (caminho.equals("/assets/estilo.css") && metodo.equals("GET")) {
                responder(requisicao, 200, "text/css; charset=utf-8", css); return;
            }
            Sessao sessao = sessao(requisicao);
            Optional<Usuario> usuario = sessao.usuarioId == 0 ? Optional.empty() : auth.buscarUsuario(sessao.usuarioId);
            if (sessao.usuarioId != 0 && !usuario.isPresent()) {
                sessoes.remove(sessao.id); sessao = novaSessao(requisicao, 0);
            }
            if (metodo.equals("POST")) {
                String origem = requisicao.getRequestHeaders().getFirst("Origin");
                if (origem != null && !origem.equals("http://" + host)) { erro(requisicao, 403, "Origem não permitida."); return; }
                Map<String, String> formulario = formulario(requisicao);
                if (!tokenIgual(sessao.csrf, formulario.get("csrf"))) { erro(requisicao, 403, "O formulário expirou. Atualize a página e tente novamente."); return; }
                receber(requisicao, caminho, sessao, usuario, formulario);
            } else {
                exibir(requisicao, caminho, sessao, usuario);
            }
        } catch (RequisicaoInvalida erro) {
            erro(requisicao, erro.status, erro.getMessage());
        } catch (RuntimeException erro) {
            System.err.println("Falha em operação local: " + erro.getClass().getSimpleName());
            erro(requisicao, 500, "Não foi possível concluir a operação. Seus registros salvos foram preservados. Tente novamente.");
        } finally { requisicao.close(); }
    }

    private void exibir(HttpExchange r, String caminho, Sessao s, Optional<Usuario> usuario) throws IOException {
        if (caminho.equals("/login") || caminho.equals("/cadastro")) {
            if (usuario.isPresent()) { redirecionar(r, "/"); return; }
            html(r, paginas.acesso(caminho.equals("/cadastro"), s.csrf, retirarMensagem(s), s.erro)); return;
        }
        if (caminho.equals("/logout") || caminho.equals("/ponto")) { erro(r, 405, "Esta ação exige um formulário."); return; }
        if (!caminho.equals("/") && !caminho.equals("/historico") && !caminho.equals("/confirmar-saida")) { erro(r, 404, "Página não encontrada."); return; }
        if (!usuario.isPresent()) { redirecionar(r, "/login"); return; }
        Usuario u = usuario.get();
        switch (caminho) {
            case "/historico":
                html(r, paginas.historico(u, s.csrf, jornadas.buscarHistorico(u.getId()), retirarMensagem(s), s.erro)); break;
            case "/confirmar-saida":
                if (!pontos.acoesDisponiveis(u.getId()).contains(TipoPonto.SAIDA)) { redirecionar(r, "/"); return; }
                html(r, paginas.confirmacao(u, s.csrf)); break;
            default:
                html(r, paginas.inicio(u, s.csrf, jornadas.buscarAtual(u.getId()), jornadas.buscarHistorico(u.getId()), pontos.acoesDisponiveis(u.getId()), retirarMensagem(s), s.erro));
        }
    }

    private void receber(HttpExchange r, String caminho, Sessao s, Optional<Usuario> usuario, Map<String, String> f) throws IOException {
        if (caminho.equals("/cadastro")) {
            Resultado<Usuario> resultado = auth.cadastrar(f.getOrDefault("nome", ""), f.getOrDefault("email", ""), f.getOrDefault("senha", ""), f.getOrDefault("confirmacao", ""));
            mensagem(s, resultado.getMensagem(), !resultado.isSucesso());
            redirecionar(r, resultado.isSucesso() ? "/login" : "/cadastro"); return;
        }
        if (caminho.equals("/login")) {
            Resultado<Usuario> resultado = auth.login(f.getOrDefault("email", ""), f.getOrDefault("senha", ""));
            if (resultado.isSucesso()) {
                sessoes.remove(s.id); novaSessao(r, resultado.getValor().getId()); redirecionar(r, "/");
            } else { mensagem(s, resultado.getMensagem(), true); redirecionar(r, "/login"); }
            return;
        }
        if (!caminho.equals("/ponto") && !caminho.equals("/logout")) { erro(r, 404, "Página não encontrada."); return; }
        if (!usuario.isPresent()) { redirecionar(r, "/login"); return; }
        if (caminho.equals("/logout")) {
            sessoes.remove(s.id); Sessao anonima = novaSessao(r, 0);
            mensagem(anonima, "Você saiu da sua conta. Seus pontos continuam salvos.", false); redirecionar(r, "/login"); return;
        }
        TipoPonto tipo;
        try { tipo = TipoPonto.valueOf(f.getOrDefault("tipo", "")); }
        catch (IllegalArgumentException erro) { mensagem(s, "Selecione uma ação de ponto válida.", true); redirecionar(r, "/"); return; }
        if (tipo == TipoPonto.SAIDA && !"sim".equals(f.get("confirmar"))) { redirecionar(r, "/confirmar-saida"); return; }
        Resultado<RegistroPonto> resultado = pontos.registrar(usuario.get().getId(), tipo);
        mensagem(s, resultado.getMensagem(), !resultado.isSucesso()); redirecionar(r, "/");
    }

    private Map<String, String> formulario(HttpExchange r) throws IOException {
        String tipo = r.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.split(";", 2)[0].trim().equalsIgnoreCase("application/x-www-form-urlencoded"))
            throw new RequisicaoInvalida(415, "Formato de formulário não suportado.");
        byte[] bytes = r.getRequestBody().readNBytes(LIMITE_CORPO + 1);
        if (bytes.length > LIMITE_CORPO) throw new RequisicaoInvalida(413, "O formulário excedeu o tamanho permitido.");
        Map<String, String> campos = new HashMap<>();
        String corpo = new String(bytes, StandardCharsets.UTF_8);
        if (corpo.isEmpty()) return campos;
        try {
            for (String par : corpo.split("&")) {
                String[] partes = par.split("=", 2);
                String chave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
                String valor = partes.length == 2 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "";
                if (campos.putIfAbsent(chave, valor) != null) throw new RequisicaoInvalida(400, "O formulário contém campos repetidos.");
            }
        } catch (IllegalArgumentException erro) { throw new RequisicaoInvalida(400, "Formulário inválido."); }
        return campos;
    }

    private Sessao sessao(HttpExchange r) {
        String cookie = r.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) for (String parte : cookie.split(";")) {
            String[] par = parte.trim().split("=", 2);
            if (par.length == 2 && par[0].equals(COOKIE)) {
                Sessao existente = sessoes.get(par[1].replace("\"", ""));
                if (existente != null) return existente;
            }
        }
        return novaSessao(r, 0);
    }

    private Sessao novaSessao(HttpExchange r, long usuarioId) {
        Sessao sessao = new Sessao(token(), token(), usuarioId);
        sessoes.put(sessao.id, sessao);
        r.getResponseHeaders().set("Set-Cookie", COOKIE + "=" + sessao.id + "; Path=/; HttpOnly; SameSite=Strict");
        return sessao;
    }

    private String token() { byte[] bytes = new byte[32]; aleatorio.nextBytes(bytes); return HexFormat.of().formatHex(bytes); }
    private static boolean tokenIgual(String esperado, String recebido) {
        return recebido != null && MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8), recebido.getBytes(StandardCharsets.UTF_8));
    }
    private static void mensagem(Sessao s, String texto, boolean erro) { s.mensagem = texto; s.erro = erro; }
    private static String retirarMensagem(Sessao s) { String texto = s.mensagem; s.mensagem = ""; return texto; }
    private static void cabecalhos(HttpExchange r) {
        r.getResponseHeaders().set("Cache-Control", "no-store");
        r.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        // Formulários locais precisam preservar Origin; no-referrer o transforma em null no navegador.
        r.getResponseHeaders().set("Referrer-Policy", "same-origin");
        r.getResponseHeaders().set("Content-Security-Policy", "default-src 'none'; style-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
    }
    private void erro(HttpExchange r, int status, String mensagem) throws IOException { responder(r, status, "text/html; charset=utf-8", paginas.erro(mensagem).getBytes(StandardCharsets.UTF_8)); }
    private static void html(HttpExchange r, String pagina) throws IOException { responder(r, 200, "text/html; charset=utf-8", pagina.getBytes(StandardCharsets.UTF_8)); }
    private static void responder(HttpExchange r, int status, String tipo, byte[] corpo) throws IOException {
        r.getResponseHeaders().set("Content-Type", tipo); r.sendResponseHeaders(status, corpo.length); r.getResponseBody().write(corpo);
    }
    private static void redirecionar(HttpExchange r, String destino) throws IOException { r.getResponseHeaders().set("Location", destino); r.sendResponseHeaders(303, -1); }

    @Override public void close() { servidor.stop(0); sessoes.clear(); banco.close(); }

    private static final class Sessao {
        final String id;
        final String csrf;
        final long usuarioId;
        String mensagem = "";
        boolean erro;
        Sessao(String id, String csrf, long usuarioId) { this.id = id; this.csrf = csrf; this.usuarioId = usuarioId; }
    }
    private static final class RequisicaoInvalida extends RuntimeException {
        final int status;
        RequisicaoInvalida(int status, String mensagem) { super(mensagem); this.status = status; }
    }
}
