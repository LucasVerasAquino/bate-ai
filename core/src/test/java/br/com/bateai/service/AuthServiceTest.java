package br.com.bateai.service;

import br.com.bateai.model.Resultado;
import br.com.bateai.model.Usuario;
import br.com.bateai.repository.EmailDuplicadoException;
import br.com.bateai.repository.PersistenciaException;
import br.com.bateai.repository.UsuarioRepository;
import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class AuthServiceTest {
    private final Clock relogio = Clock.fixed(Instant.parse("2026-09-30T11:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private MemoriaUsuarios usuarios;
    private AuthService auth;

    @Before public void preparar() {
        usuarios = new MemoriaUsuarios();
        auth = new AuthService(usuarios, relogio);
    }

    @Test public void cadastrar_dadosValidos_normalizaEmailEPersisteHash() {
        Resultado<Usuario> resultado = auth.cadastrar("  Ana Silva  ", " ANA@EXEMPLO.COM ", "senha123", "senha123");
        assertTrue(resultado.getMensagem(), resultado.isSucesso());
        Usuario usuario = resultado.getValor();
        assertEquals("Ana Silva", usuario.getNome());
        assertEquals("ana@exemplo.com", usuario.getEmail());
        assertEquals(relogio.millis(), usuario.getCriadoEm());
        assertEquals(16, usuario.getSalt().length);
        assertEquals(32, usuario.getSenhaHash().length);
        assertTrue(usuario.getId() > 0);
        assertTrue(auth.buscarUsuario(usuario.getId()).isPresent());
        assertFalse(Arrays.equals("senha123".getBytes(java.nio.charset.StandardCharsets.UTF_8), usuario.getSenhaHash()));
    }

    @Test public void cadastrar_emailDuplicadoComMaiusculas_rejeita() {
        assertTrue(auth.cadastrar("Ana", "ana@exemplo.com", "senha123", "senha123").isSucesso());
        Resultado<Usuario> resultado = auth.cadastrar("Outra Ana", " ANA@EXEMPLO.COM ", "senha456", "senha456");
        assertFalse(resultado.isSucesso());
        assertTrue(resultado.getMensagem().contains("cadastrado"));
        assertEquals(1, usuarios.dados.size());
    }

    @Test public void cadastrar_camposInvalidos_naoPersiste() {
        assertFalse(auth.cadastrar(" ", "ana@exemplo.com", "senha123", "senha123").isSucesso());
        assertFalse(auth.cadastrar("Ana", "invalido", "senha123", "senha123").isSucesso());
        assertFalse(auth.cadastrar("Ana", "ana @exemplo.com", "senha123", "senha123").isSucesso());
        assertFalse(auth.cadastrar("Ana", "ana@exemplo.com", "curta", "curta").isSucesso());
        assertFalse(auth.cadastrar("Ana", "ana@exemplo.com", "senha123", "senha456").isSucesso());
        assertFalse(auth.cadastrar(null, null, null, null).isSucesso());
        assertTrue(usuarios.dados.isEmpty());
    }

    @Test public void login_credenciaisCorretas_retornaUsuario() {
        Usuario salvo = auth.cadastrar("Ana", "ana@exemplo.com", "senha123", "senha123").getValor();
        Resultado<Usuario> resultado = auth.login(" ANA@EXEMPLO.COM ", "senha123");
        assertTrue(resultado.getMensagem(), resultado.isSucesso());
        assertEquals(salvo.getId(), resultado.getValor().getId());
    }

    @Test public void login_senhaIncorretaOuUsuarioInexistente_rejeita() {
        auth.cadastrar("Ana", "ana@exemplo.com", "senha123", "senha123");
        Resultado<Usuario> senhaErrada = auth.login("ana@exemplo.com", "errada123");
        Resultado<Usuario> inexistente = auth.login("outra@exemplo.com", "senha123");
        assertFalse(senhaErrada.isSucesso());
        assertFalse(inexistente.isSucesso());
        assertEquals(senhaErrada.getMensagem(), inexistente.getMensagem());
        assertFalse(auth.login(null, null).isSucesso());
        assertFalse(auth.buscarUsuario(999).isPresent());
    }

    @Test public void cadastrar_mesmaSenhaEmContasDistintas_geraSaltEHashDistintos() {
        Usuario ana = auth.cadastrar("Ana", "ana@exemplo.com", "senha123", "senha123").getValor();
        Usuario bia = auth.cadastrar("Bia", "bia@exemplo.com", "senha123", "senha123").getValor();
        assertFalse(Arrays.equals(ana.getSalt(), bia.getSalt()));
        assertFalse(Arrays.equals(ana.getSenhaHash(), bia.getSenhaHash()));
    }

    @Test public void cadastrar_conflitoConcorrente_retornaErroDeEmailDuplicado() {
        usuarios.falhaAoInserir = new EmailDuplicadoException("conflito");
        Resultado<Usuario> resultado = auth.cadastrar("Ana", "ana@exemplo.com", "senha123", "senha123");
        assertFalse(resultado.isSucesso());
        assertTrue(resultado.getMensagem().contains("cadastrado"));
    }

    @Test public void cadastrar_falhaTecnica_naoInformaSucesso() {
        usuarios.falhaAoInserir = new PersistenciaException("disco indisponível");
        Resultado<Usuario> resultado = auth.cadastrar("Ana", "ana@exemplo.com", "senha123", "senha123");
        assertFalse(resultado.isSucesso());
        assertTrue(usuarios.dados.isEmpty());
    }

    static class MemoriaUsuarios implements UsuarioRepository {
        final List<Usuario> dados = new ArrayList<>();
        RuntimeException falhaAoInserir;
        @Override public Optional<Usuario> buscarPorId(long id) { return dados.stream().filter(u -> u.getId() == id).findFirst(); }
        @Override public Optional<Usuario> buscarPorEmail(String email) { return dados.stream().filter(u -> u.getEmail().equals(email)).findFirst(); }
        @Override public Usuario inserir(Usuario usuario) {
            if (falhaAoInserir != null) throw falhaAoInserir;
            if (buscarPorEmail(usuario.getEmail()).isPresent()) throw new EmailDuplicadoException("duplicado");
            Usuario salvo = new Usuario(dados.size() + 1L, usuario.getNome(), usuario.getEmail(), usuario.getSenhaHash(), usuario.getSalt(), usuario.getCriadoEm());
            dados.add(salvo);
            return salvo;
        }
    }
}
