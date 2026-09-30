package br.com.bateai.ui;

import android.content.Context;
import android.content.SharedPreferences;

public final class SessaoLocal {
    private final SharedPreferences preferencias;

    public SessaoLocal(Context context) { this(context, "sessao"); }

    public SessaoLocal(Context context, String nome) {
        preferencias = context.getApplicationContext().getSharedPreferences(nome, Context.MODE_PRIVATE);
    }

    public long usuarioId() { return preferencias.getLong("usuario_logado_id", 0L); }

    public void iniciar(long id) {
        if (id <= 0L) throw new IllegalArgumentException("Identificador de conta inválido.");
        if (!preferencias.edit().putLong("usuario_logado_id", id).commit()) {
            throw new IllegalStateException("Não foi possível salvar a sessão. Tente entrar novamente.");
        }
    }

    public void encerrar() {
        if (!preferencias.edit().remove("usuario_logado_id").commit()) {
            throw new IllegalStateException("Não foi possível encerrar a sessão. Tente novamente.");
        }
    }
}
