package br.com.bateai.service;

import br.com.bateai.model.Jornada;
import br.com.bateai.model.RegistroPonto;
import br.com.bateai.model.TipoPonto;
import br.com.bateai.repository.PontoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public final class JornadaService {
    private final PontoRepository pontos;
    private final Clock relogio;

    public JornadaService(PontoRepository pontos, Clock relogio) {
        this.pontos = Objects.requireNonNull(pontos);
        this.relogio = Objects.requireNonNull(relogio);
    }

    public List<Jornada> buscarHistorico(long usuarioId) {
        TreeMap<LocalDate, List<RegistroPonto>> porData = new TreeMap<>();
        for (RegistroPonto registro : pontos.listar(usuarioId)) {
            porData.computeIfAbsent(registro.getDataJornada(), data -> new ArrayList<>()).add(registro);
        }
        long agora = relogio.millis();
        List<Jornada> jornadas = new ArrayList<>();
        for (Map.Entry<LocalDate, List<RegistroPonto>> dia : porData.descendingMap().entrySet()) {
            jornadas.add(calcular(dia.getKey(), dia.getValue(), agora));
        }
        return Collections.unmodifiableList(jornadas);
    }

    public Optional<Jornada> buscarAtual(long usuarioId) {
        List<Jornada> historico = buscarHistorico(usuarioId);
        for (Jornada jornada : historico) {
            if (jornada.isAberta()) return Optional.of(jornada);
        }
        LocalDate hoje = LocalDate.now(relogio);
        for (Jornada jornada : historico) {
            if (jornada.getData().equals(hoje)) return Optional.of(jornada);
        }
        return Optional.empty();
    }

    private static Jornada calcular(LocalDate data, List<RegistroPonto> registros, long agora) {
        RegistroPonto entrada = registros.get(0);
        RegistroPonto ultimo = registros.get(registros.size() - 1);
        boolean aberta = ultimo.getTipo() != TipoPonto.SAIDA;
        // Um ajuste do relógio não deve apagar o tempo já registrado.
        long fimJornada = aberta ? Math.max(agora, ultimo.getTimestamp()) : ultimo.getTimestamp();
        long intervalo = 0;
        long inicioIntervalo = 0;
        boolean emIntervalo = false;
        for (RegistroPonto registro : registros) {
            if (registro.getTipo() == TipoPonto.INICIO_INTERVALO) {
                inicioIntervalo = registro.getTimestamp();
                emIntervalo = true;
            } else if (registro.getTipo() == TipoPonto.FIM_INTERVALO) {
                intervalo = registro.getTimestamp() - inicioIntervalo;
                emIntervalo = false;
            }
        }
        if (emIntervalo) intervalo = fimJornada - inicioIntervalo;
        long trabalhado = Math.max(0, fimJornada - entrada.getTimestamp() - intervalo);
        String status = !aberta ? "Encerrada" : emIntervalo ? "Em intervalo" : "Trabalhando";
        return new Jornada(data, registros, trabalhado, Math.max(0, intervalo), status, aberta);
    }
}
