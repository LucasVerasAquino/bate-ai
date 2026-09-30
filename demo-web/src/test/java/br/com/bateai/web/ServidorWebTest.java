package br.com.bateai.web;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

public class ServidorWebTest {
    @Rule public TemporaryFolder pasta = new TemporaryFolder();
    private final Clock relogio = Clock.fixed(Instant.parse("2026-09-30T11:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final Pattern token = Pattern.compile("name=\"csrf\" value=\"([^\"]+)\"");

    @Test public void fluxoCompleto_persistePontosConfirmaSaidaEEncerraSessao() throws Exception {
        Path db = pasta.getRoot().toPath().resolve("web.db");
        try (ServidorWeb servidor = new ServidorWeb(db, relogio, 0)) {
            servidor.iniciar();
            Navegador navegador = new Navegador(servidor.getPorta());
            assertEquals(303, navegador.get("/historico").statusCode());
            cadastrar(navegador, "Ana <script>", "ana@example.com");
            HttpResponse<String> login = entrar(navegador, "ana@example.com");
            assertEquals(303, login.statusCode());
            String cookie = login.headers().firstValue("Set-Cookie").get();
            assertTrue(cookie.contains("HttpOnly"));
            assertTrue(cookie.contains("SameSite=Strict"));
            String pagina = navegador.get("/").body();
            assertTrue(pagina.contains("Ana &lt;script&gt;"));
            assertFalse(pagina.contains("Ana <script>"));
            assertEquals(303, navegador.post("/ponto", "csrf=" + csrf(pagina) + "&tipo=ENTRADA").statusCode());
            pagina = navegador.get("/").body();
            assertTrue(pagina.contains("Trabalhando"));
            assertTrue(navegador.get("/confirmar-saida").body().contains("Confirmar saída"));
            HttpResponse<String> semConfirmacao = navegador.post("/ponto", "csrf=" + csrf(pagina) + "&tipo=SAIDA");
            assertEquals("/confirmar-saida", semConfirmacao.headers().firstValue("Location").get());
            assertTrue(navegador.get("/").body().contains("Trabalhando"));
            String confirmar = navegador.get("/confirmar-saida").body();
            assertEquals(303, navegador.post("/ponto", "csrf=" + csrf(confirmar) + "&tipo=SAIDA&confirmar=sim").statusCode());
            assertTrue(navegador.get("/historico").body().contains("30/09/2026"));
            pagina = navegador.get("/").body();
            assertEquals(303, navegador.post("/logout", "csrf=" + csrf(pagina)).statusCode());
            assertEquals(303, navegador.get("/historico").statusCode());
        }
        try (ServidorWeb servidor = new ServidorWeb(db, relogio, 0)) {
            servidor.iniciar();
            Navegador navegador = new Navegador(servidor.getPorta());
            assertEquals(303, entrar(navegador, "ana@example.com").statusCode());
            String historico = navegador.get("/historico").body();
            assertTrue(historico.contains("Entrada"));
            assertTrue(historico.contains("Saída"));
        }
    }

    @Test public void csrfAusenteOuInvalido_impedeCadastroLoginPontoELogout() throws Exception {
        try (ServidorWeb servidor = new ServidorWeb(pasta.getRoot().toPath().resolve("web.db"), relogio, 0)) {
            servidor.iniciar();
            Navegador navegador = new Navegador(servidor.getPorta());
            navegador.get("/cadastro");
            assertEquals(403, navegador.post("/cadastro", "nome=Ana&email=ana@example.com&senha=senha1234&confirmacao=senha1234").statusCode());
            cadastrar(navegador, "Ana", "ana@example.com");
            assertEquals(403, navegador.post("/login", "csrf=invalido&email=ana@example.com&senha=senha1234").statusCode());
            entrar(navegador, "ana@example.com");
            assertEquals(403, navegador.post("/ponto", "tipo=ENTRADA").statusCode());
            assertEquals(403, navegador.post("/logout", "csrf=invalido").statusCode());
            assertEquals(200, navegador.get("/").statusCode());
        }
    }

    @Test public void duasContas_naoCompartilhamHistorico() throws Exception {
        try (ServidorWeb servidor = new ServidorWeb(pasta.getRoot().toPath().resolve("web.db"), relogio, 0)) {
            servidor.iniciar();
            Navegador ana = new Navegador(servidor.getPorta());
            cadastrar(ana, "Ana", "ana@example.com"); entrar(ana, "ana@example.com");
            ana.post("/ponto", "csrf=" + csrf(ana.get("/").body()) + "&tipo=ENTRADA");
            Navegador bia = new Navegador(servidor.getPorta());
            cadastrar(bia, "Bia", "bia@example.com"); entrar(bia, "bia@example.com");
            assertTrue(bia.get("/historico").body().contains("Seu histórico começa aqui"));
            assertTrue(ana.get("/historico").body().contains("30/09/2026"));
        }
    }

    @Test public void reiniciarServidor_invalidaCookieAnteriorEPreservaConta() throws Exception {
        Path db = pasta.getRoot().toPath().resolve("web.db");
        Navegador navegador;
        int porta;
        try (ServidorWeb servidor = new ServidorWeb(db, relogio, 0)) {
            servidor.iniciar(); porta = servidor.getPorta(); navegador = new Navegador(porta);
            cadastrar(navegador, "Ana", "ana@example.com"); entrar(navegador, "ana@example.com");
            assertEquals(200, navegador.get("/").statusCode());
        }
        try (ServidorWeb servidor = new ServidorWeb(db, relogio, porta)) {
            servidor.iniciar();
            assertEquals(303, navegador.get("/").statusCode());
            assertEquals(303, entrar(navegador, "ana@example.com").statusCode());
        }
    }

    @Test public void requisicoesInvalidas_retornamErroSemExecutarOperacoes() throws Exception {
        try (ServidorWeb servidor = new ServidorWeb(pasta.getRoot().toPath().resolve("web.db"), relogio, 0)) {
            servidor.iniciar();
            Navegador navegador = new Navegador(servidor.getPorta());
            String csrf = csrf(navegador.get("/cadastro").body());
            assertEquals(400, navegador.post("/cadastro", "csrf=" + csrf + "&nome=a&nome=b").statusCode());
            assertEquals(413, navegador.post("/cadastro", "csrf=" + csrf + "&nome=" + "a".repeat(17000)).statusCode());
            assertEquals(404, navegador.get("/inexistente").statusCode());
            assertEquals(405, navegador.get("/logout").statusCode());
            HttpResponse<String> origem = navegador.cliente.send(HttpRequest.newBuilder(URI.create(navegador.base + "/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded").header("Origin", "https://outro.example")
                    .POST(HttpRequest.BodyPublishers.ofString("csrf=" + csrf)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(403, origem.statusCode());
            HttpResponse<String> pagina = navegador.get("/login");
            assertTrue(pagina.headers().firstValue("Content-Security-Policy").get().contains("default-src 'none'"));
            assertFalse(pagina.body().contains("https://"));
        }
    }

    private void cadastrar(Navegador n, String nome, String email) throws Exception {
        String pagina = n.get("/cadastro").body();
        assertEquals(303, n.post("/cadastro", "csrf=" + csrf(pagina) + "&nome=" + enc(nome) + "&email=" + enc(email) + "&senha=senha1234&confirmacao=senha1234").statusCode());
    }
    private HttpResponse<String> entrar(Navegador n, String email) throws Exception {
        return n.post("/login", "csrf=" + csrf(n.get("/login").body()) + "&email=" + enc(email) + "&senha=senha1234");
    }
    private String csrf(String html) { Matcher m = token.matcher(html); assertTrue("Formulário precisa de CSRF", m.find()); return m.group(1); }
    private String enc(String valor) { return URLEncoder.encode(valor, StandardCharsets.UTF_8); }
    private static class Navegador {
        final String base;
        final HttpClient cliente = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        Navegador(int porta) { base = "http://127.0.0.1:" + porta; }
        HttpResponse<String> get(String path) throws Exception {
            return cliente.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> post(String path, String dados) throws Exception {
            return cliente.send(HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(dados)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
