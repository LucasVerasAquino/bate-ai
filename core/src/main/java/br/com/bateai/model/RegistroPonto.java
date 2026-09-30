package br.com.bateai.model;

import java.time.LocalDate;
import java.util.Objects;

public final class RegistroPonto {
    private final long id;
    private final long usuarioId;
    private final TipoPonto tipo;
    private final long timestamp;
    private final LocalDate dataJornada;

    public RegistroPonto(long id, long usuarioId, TipoPonto tipo, long timestamp, LocalDate dataJornada) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tipo = Objects.requireNonNull(tipo);
        this.timestamp = timestamp;
        this.dataJornada = Objects.requireNonNull(dataJornada);
    }

    public long getId() { return id; }
    public long getUsuarioId() { return usuarioId; }
    public TipoPonto getTipo() { return tipo; }
    public long getTimestamp() { return timestamp; }
    public LocalDate getDataJornada() { return dataJornada; }
}
