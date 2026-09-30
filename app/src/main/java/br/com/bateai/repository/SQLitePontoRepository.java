package br.com.bateai.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import br.com.bateai.database.DatabaseHelper;
import br.com.bateai.model.RegistroPonto;
import br.com.bateai.model.TipoPonto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class SQLitePontoRepository implements PontoRepository {
    private final DatabaseHelper helper;

    public SQLitePontoRepository(DatabaseHelper helper) { this.helper = helper; }

    @Override public List<RegistroPonto> listar(long usuarioId) {
        List<RegistroPonto> pontos = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query("registros_ponto",
                new String[]{"id", "usuario_id", "tipo", "timestamp", "data_jornada"},
                "usuario_id = ?", new String[]{String.valueOf(usuarioId)}, null, null, "timestamp ASC, id ASC")) {
            while (cursor.moveToNext()) {
                pontos.add(new RegistroPonto(cursor.getLong(0), cursor.getLong(1),
                        TipoPonto.valueOf(cursor.getString(2)), cursor.getLong(3),
                        LocalDate.parse(cursor.getString(4))));
            }
            return pontos;
        } catch (SQLiteException ex) {
            throw new PersistenciaException("Não foi possível consultar os pontos.", ex);
        }
    }

    @Override public RegistroPonto inserir(RegistroPonto ponto) {
        ContentValues dados = new ContentValues();
        dados.put("usuario_id", ponto.getUsuarioId());
        dados.put("tipo", ponto.getTipo().name());
        dados.put("timestamp", ponto.getTimestamp());
        dados.put("data_jornada", ponto.getDataJornada().toString());
        try {
            long id = helper.getWritableDatabase().insertOrThrow("registros_ponto", null, dados);
            return new RegistroPonto(id, ponto.getUsuarioId(), ponto.getTipo(), ponto.getTimestamp(), ponto.getDataJornada());
        } catch (SQLiteException ex) {
            throw new PersistenciaException("Não foi possível salvar o ponto. Tente novamente.", ex);
        }
    }

    @Override public <T> T emTransacao(Supplier<T> operacao) {
        try {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                T resultado = operacao.get();
                db.setTransactionSuccessful();
                return resultado;
            } finally {
                db.endTransaction();
            }
        } catch (SQLiteException ex) {
            throw new PersistenciaException("Não foi possível concluir o registro do ponto.", ex);
        }
    }
}
