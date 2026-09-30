package br.com.bateai.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import br.com.bateai.database.DatabaseHelper;
import br.com.bateai.model.Usuario;
import java.util.Optional;

public final class SQLiteUsuarioRepository implements UsuarioRepository {
    private final DatabaseHelper helper;

    public SQLiteUsuarioRepository(DatabaseHelper helper) { this.helper = helper; }

    @Override public Optional<Usuario> buscarPorId(long id) {
        return buscar("id = ?", String.valueOf(id));
    }

    @Override public Optional<Usuario> buscarPorEmail(String email) {
        return buscar("email = ?", email);
    }

    private Optional<Usuario> buscar(String filtro, String valor) {
        try (Cursor cursor = helper.getReadableDatabase().query("usuarios",
                new String[]{"id", "nome", "email", "senha_hash", "salt", "criado_em"},
                filtro, new String[]{valor}, null, null, null)) {
            if (!cursor.moveToFirst()) return Optional.empty();
            return Optional.of(new Usuario(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                    cursor.getBlob(3), cursor.getBlob(4), cursor.getLong(5)));
        } catch (SQLiteException ex) {
            throw new PersistenciaException("Não foi possível consultar a conta.", ex);
        }
    }

    @Override public Usuario inserir(Usuario usuario) {
        ContentValues dados = new ContentValues();
        dados.put("nome", usuario.getNome());
        dados.put("email", usuario.getEmail());
        dados.put("senha_hash", usuario.getSenhaHash());
        dados.put("salt", usuario.getSalt());
        dados.put("criado_em", usuario.getCriadoEm());
        try {
            long id = helper.getWritableDatabase().insertOrThrow("usuarios", null, dados);
            return new Usuario(id, usuario.getNome(), usuario.getEmail(), usuario.getSenhaHash(),
                    usuario.getSalt(), usuario.getCriadoEm());
        } catch (SQLiteConstraintException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("usuarios.email")) {
                throw new EmailDuplicadoException("Este e-mail já possui uma conta.", ex);
            }
            throw new PersistenciaException("Não foi possível salvar a conta.", ex);
        } catch (SQLiteException ex) {
            throw new PersistenciaException("Não foi possível salvar a conta.", ex);
        }
    }
}
