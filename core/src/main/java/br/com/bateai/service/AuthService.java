package br.com.bateai.service;

import br.com.bateai.model.Resultado;
import br.com.bateai.model.Usuario;
import br.com.bateai.repository.EmailDuplicadoException;
import br.com.bateai.repository.PersistenciaException;
import br.com.bateai.repository.UsuarioRepository;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class AuthService {
    private static final int ITERACOES = 600_000;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha incorretos.";
    private static final String EMAIL_DUPLICADO = "Este e-mail já está cadastrado.";
    private final UsuarioRepository usuarios;
    private final Clock relogio;
    private final SecureRandom aleatorio = new SecureRandom();

    public AuthService(UsuarioRepository usuarios, Clock relogio) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.relogio = Objects.requireNonNull(relogio);
    }

    public Resultado<Usuario> cadastrar(String nome, String email, String senha, String confirmacao) {
        String nomeLimpo = nome == null ? "" : nome.trim();
        String emailLimpo = normalizarEmail(email);
        if (nomeLimpo.isEmpty()) return Resultado.erro("Informe seu nome.");
        if (!EMAIL.matcher(emailLimpo).matches()) return Resultado.erro("Informe um e-mail válido.");
        if (senha == null || senha.length() < 8) return Resultado.erro("A senha deve ter pelo menos oito caracteres.");
        if (!senha.equals(confirmacao)) return Resultado.erro("A confirmação da senha não confere.");
        try {
            if (usuarios.buscarPorEmail(emailLimpo).isPresent()) return Resultado.erro(EMAIL_DUPLICADO);
            byte[] salt = new byte[16];
            aleatorio.nextBytes(salt);
            byte[] hash = gerarHash(senha, salt);
            Usuario salvo = usuarios.inserir(new Usuario(0, nomeLimpo, emailLimpo, hash, salt, relogio.millis()));
            return Resultado.sucesso(salvo, "Conta criada com sucesso. Entre para começar.");
        } catch (EmailDuplicadoException erro) {
            return Resultado.erro(EMAIL_DUPLICADO);
        } catch (PersistenciaException erro) {
            return Resultado.erro("Não foi possível salvar a conta. Tente novamente.");
        } catch (GeneralSecurityException erro) {
            return Resultado.erro("Não foi possível proteger a senha neste dispositivo.");
        }
    }

    public Resultado<Usuario> login(String email, String senha) {
        if (senha == null || senha.isEmpty()) return Resultado.erro(CREDENCIAIS_INVALIDAS);
        try {
            Optional<Usuario> encontrado = usuarios.buscarPorEmail(normalizarEmail(email));
            if (!encontrado.isPresent()) return Resultado.erro(CREDENCIAIS_INVALIDAS);
            Usuario usuario = encontrado.get();
            byte[] hash = gerarHash(senha, usuario.getSalt());
            if (!MessageDigest.isEqual(hash, usuario.getSenhaHash())) return Resultado.erro(CREDENCIAIS_INVALIDAS);
            return Resultado.sucesso(usuario, "Bem-vindo, " + usuario.getNome() + "!");
        } catch (PersistenciaException erro) {
            return Resultado.erro("Não foi possível acessar os dados. Tente novamente.");
        } catch (GeneralSecurityException erro) {
            return Resultado.erro("Não foi possível verificar a senha neste dispositivo.");
        }
    }

    public Optional<Usuario> buscarUsuario(long id) { return usuarios.buscarPorId(id); }

    private static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] gerarHash(String senha, byte[] salt) throws GeneralSecurityException {
        char[] caracteres = senha.toCharArray();
        PBEKeySpec parametros = new PBEKeySpec(caracteres, salt, ITERACOES, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(parametros).getEncoded();
        } finally {
            parametros.clearPassword();
            Arrays.fill(caracteres, '\0');
        }
    }
}
