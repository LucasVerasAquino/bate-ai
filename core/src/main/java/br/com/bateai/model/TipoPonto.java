package br.com.bateai.model;

public enum TipoPonto {
    ENTRADA("Entrada"),
    INICIO_INTERVALO("Início do intervalo"),
    FIM_INTERVALO("Fim do intervalo"),
    SAIDA("Saída");

    private final String descricao;
    TipoPonto(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
