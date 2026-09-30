package br.com.bateai.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context context) { this(context, "bate-ai.db"); }

    public DatabaseHelper(Context context, String nome) {
        super(context.getApplicationContext(), nome, null, 1);
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nome TEXT NOT NULL, email TEXT NOT NULL UNIQUE, senha_hash BLOB NOT NULL, " +
                "salt BLOB NOT NULL, criado_em INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE registros_ponto (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "usuario_id INTEGER NOT NULL REFERENCES usuarios(id), tipo TEXT NOT NULL " +
                "CHECK(tipo IN ('ENTRADA','INICIO_INTERVALO','FIM_INTERVALO','SAIDA')), " +
                "timestamp INTEGER NOT NULL, data_jornada TEXT NOT NULL, " +
                "UNIQUE(usuario_id, data_jornada, tipo))");
        db.execSQL("CREATE INDEX idx_pontos_usuario_horario ON registros_ponto(usuario_id, timestamp, id)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int antiga, int nova) {
        throw new IllegalStateException("Migração de banco não disponível: " + antiga + " para " + nova);
    }
}
