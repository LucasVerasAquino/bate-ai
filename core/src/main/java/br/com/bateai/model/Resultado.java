package br.com.bateai.model;

import java.util.Objects;

public final class Resultado<T> {
    private final T valor;
    private final String mensagem;
    private final boolean sucesso;

    private Resultado(T valor, String mensagem, boolean sucesso) {
        this.valor = valor;
        this.mensagem = Objects.requireNonNull(mensagem);
        this.sucesso = sucesso;
    }

    public static <T> Resultado<T> sucesso(T valor, String mensagem) {
        return new Resultado<>(Objects.requireNonNull(valor), mensagem, true);
    }

    public static <T> Resultado<T> erro(String mensagem) {
        return new Resultado<>(null, mensagem, false);
    }

    public boolean isSucesso() { return sucesso; }
    public String getMensagem() { return mensagem; }
    public T getValor() {
        if (!sucesso) throw new IllegalStateException("Um resultado de erro não contém valor.");
        return valor;
    }
}
