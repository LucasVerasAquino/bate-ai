package br.com.bateai.web.repository;

import br.com.bateai.model.RegistroPonto;
import br.com.bateai.model.Usuario;
import br.com.bateai.model.TipoPonto;
import br.com.bateai.repository.EmailDuplicadoException;
import br.com.bateai.repository.PontoRepository;
import br.com.bateai.repository.UsuarioRepository;
import br.com.bateai.web.database.BancoSqlite;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class SqliteRepository implements UsuarioRepository, PontoRepository, AutoCloseable {
    private final BancoSqlite banco;
    public SqliteRepository(Path arquivo) { banco = new BancoSqlite(arquivo); }

    @Override public Optional<Usuario> buscarPorId(long id) { return buscarUsuario("SELECT * FROM usuarios WHERE id=?", id); }
    @Override public Optional<Usuario> buscarPorEmail(String email) { return buscarUsuario("SELECT * FROM usuarios WHERE email=?", email); }

    private Optional<Usuario> buscarUsuario(String sql, Object valor) {
        return banco.executar(c -> {
            try (PreparedStatement comando = c.prepareStatement(sql)) {
                comando.setObject(1, valor);
                try (ResultSet r = comando.executeQuery()) {
                    return r.next() ? Optional.of(new Usuario(r.getLong("id"), r.getString("nome"), r.getString("email"), r.getBytes("senha_hash"), r.getBytes("salt"), r.getLong("criado_em"))) : Optional.empty();
                }
            }
        });
    }

    @Override public Usuario inserir(Usuario usuario) {
        return banco.executar(c -> {
            try (PreparedStatement comando = c.prepareStatement("INSERT INTO usuarios(nome,email,senha_hash,salt,criado_em) VALUES(?,?,?,?,?)")) {
                comando.setString(1, usuario.getNome()); comando.setString(2, usuario.getEmail());
                comando.setBytes(3, usuario.getSenhaHash()); comando.setBytes(4, usuario.getSalt()); comando.setLong(5, usuario.getCriadoEm());
                comando.executeUpdate();
                try (Statement identidade = c.createStatement(); ResultSet r = identidade.executeQuery("SELECT last_insert_rowid()")) {
                    r.next(); return new Usuario(r.getLong(1), usuario.getNome(), usuario.getEmail(), usuario.getSenhaHash(), usuario.getSalt(), usuario.getCriadoEm());
                }
            } catch (SQLException erro) {
                if (erro.getMessage() != null && erro.getMessage().contains("UNIQUE constraint failed: usuarios.email"))
                    throw new EmailDuplicadoException("Este e-mail já está cadastrado.", erro);
                throw erro;
            }
        });
    }

    @Override public RegistroPonto inserir(RegistroPonto ponto) {
        return banco.executar(c -> {
            try (PreparedStatement comando = c.prepareStatement("INSERT INTO registros_ponto(usuario_id,tipo,timestamp,data_jornada) VALUES(?,?,?,?)")) {
                comando.setLong(1, ponto.getUsuarioId()); comando.setString(2, ponto.getTipo().name());
                comando.setLong(3, ponto.getTimestamp()); comando.setString(4, ponto.getDataJornada().toString()); comando.executeUpdate();
                try (Statement identidade = c.createStatement(); ResultSet r = identidade.executeQuery("SELECT last_insert_rowid()")) {
                    r.next(); return new RegistroPonto(r.getLong(1), ponto.getUsuarioId(), ponto.getTipo(), ponto.getTimestamp(), ponto.getDataJornada());
                }
            }
        });
    }

    @Override public List<RegistroPonto> listar(long usuarioId) {
        return banco.executar(c -> {
            List<RegistroPonto> pontos = new ArrayList<>();
            try (PreparedStatement comando = c.prepareStatement("SELECT * FROM registros_ponto WHERE usuario_id=? ORDER BY timestamp,id")) {
                comando.setLong(1, usuarioId);
                try (ResultSet r = comando.executeQuery()) {
                    while (r.next()) pontos.add(new RegistroPonto(r.getLong("id"), r.getLong("usuario_id"), TipoPonto.valueOf(r.getString("tipo")), r.getLong("timestamp"), LocalDate.parse(r.getString("data_jornada"))));
                }
            }
            return pontos;
        });
    }

    @Override public <T> T emTransacao(Supplier<T> acao) { return banco.emTransacao(acao); }
    @Override public void close() { banco.close(); }
}
