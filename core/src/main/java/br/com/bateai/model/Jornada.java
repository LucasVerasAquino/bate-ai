package br.com.bateai.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Jornada {
    private final LocalDate data;
    private final List<RegistroPonto> registros;
    private final long trabalhadoMillis;
    private final long intervaloMillis;
    private final String status;
    private final boolean aberta;

    public Jornada(LocalDate data, List<RegistroPonto> registros, long trabalhadoMillis,
                   long intervaloMillis, String status, boolean aberta) {
        this.data = Objects.requireNonNull(data);
        this.registros = Collections.unmodifiableList(new ArrayList<>(registros));
        this.trabalhadoMillis = trabalhadoMillis;
        this.intervaloMillis = intervaloMillis;
        this.status = Objects.requireNonNull(status);
        this.aberta = aberta;
    }

    public LocalDate getData() { return data; }
    public List<RegistroPonto> getRegistros() { return registros; }
    public long getTrabalhadoMillis() { return trabalhadoMillis; }
    public long getIntervaloMillis() { return intervaloMillis; }
    public String getStatus() { return status; }
    public boolean isAberta() { return aberta; }
}
