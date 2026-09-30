package br.com.bateai.web.database;

import br.com.bateai.repository.PersistenciaException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/** Uma conexão local: o mesmo monitor protege consultas e a transação de uma batida. */
public final class BancoSqlite implements AutoCloseable {
    private final Connection conexao;

    public BancoSqlite(Path arquivo) {
        Connection aberta = null;
        try {
            Path absoluto = arquivo.toAbsolutePath().normalize();
            Files.createDirectories(absoluto.getParent());
            aberta = DriverManager.getConnection("jdbc:sqlite:" + absoluto);
            configurar(aberta);
            conexao = aberta;
        } catch (SQLException | IOException | RuntimeException erro) {
            if (aberta != null) try { aberta.close(); } catch (SQLException falha) { erro.addSuppressed(falha); }
            throw new PersistenciaException("Não foi possível abrir o banco local.", erro);
        }
    }

    private static void configurar(Connection conexao) throws SQLException {
        try (Statement comando = conexao.createStatement()) {
            comando.execute("PRAGMA foreign_keys=ON");
            comando.execute("PRAGMA busy_timeout=5000");
            int versao;
            try (ResultSet resultado = comando.executeQuery("PRAGMA user_version")) { resultado.next(); versao = resultado.getInt(1); }
            if (versao > 1) throw new SQLException("Versão do banco mais recente que o aplicativo; dados preservados.");
            if (versao == 1) return;
            conexao.setAutoCommit(false);
            try {
                comando.execute("CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL, email TEXT NOT NULL UNIQUE, senha_hash BLOB NOT NULL, salt BLOB NOT NULL, criado_em INTEGER NOT NULL)");
                comando.execute("CREATE TABLE registros_ponto (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER NOT NULL REFERENCES usuarios(id), tipo TEXT NOT NULL CHECK(tipo IN ('ENTRADA','INICIO_INTERVALO','FIM_INTERVALO','SAIDA')), timestamp INTEGER NOT NULL, data_jornada TEXT NOT NULL, UNIQUE(usuario_id,data_jornada,tipo))");
                comando.execute("CREATE INDEX idx_pontos_usuario_timestamp ON registros_ponto(usuario_id,timestamp,id)");
                comando.execute("PRAGMA user_version=1");
                conexao.commit();
            } catch (SQLException erro) {
                try { conexao.rollback(); } catch (SQLException falha) { erro.addSuppressed(falha); }
                throw erro;
            } finally { conexao.setAutoCommit(true); }
        }
    }

    @FunctionalInterface public interface Consulta<T> { T executar(Connection conexao) throws SQLException; }

    public synchronized <T> T executar(Consulta<T> consulta) {
        try { return consulta.executar(conexao); }
        catch (SQLException erro) { throw new PersistenciaException("Não foi possível acessar os dados locais.", erro); }
    }

    public synchronized <T> T emTransacao(Supplier<T> operacao) {
        return executar(c -> {
            if (!c.getAutoCommit()) return operacao.get();
            c.setAutoCommit(false);
            try {
                T resultado = operacao.get();
                c.commit();
                return resultado;
            } catch (RuntimeException | Error | SQLException erro) {
                try { c.rollback(); } catch (SQLException falha) { erro.addSuppressed(falha); }
                throw erro;
            } finally { c.setAutoCommit(true); }
        });
    }

    @Override public synchronized void close() {
        try { conexao.close(); }
        catch (SQLException erro) { throw new PersistenciaException("Não foi possível fechar o banco local.", erro); }
    }
}
