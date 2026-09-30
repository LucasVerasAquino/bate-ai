package br.com.bateai.service;

import br.com.bateai.model.Jornada;
import br.com.bateai.model.RegistroPonto;
import br.com.bateai.model.TipoPonto;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static br.com.bateai.model.TipoPonto.*;
import static org.junit.Assert.*;

public class JornadaServiceTest {
    private AmbientePontos ambiente;
    private JornadaService service;

    @Before public void preparar() {
        ambiente = new AmbientePontos();
        service = new JornadaService(ambiente.pontos, ambiente.relogio);
    }

    @Test public void buscarHistorico_semRegistros_retornaVazio() {
        assertTrue(service.buscarHistorico(ambiente.ana).isEmpty());
        assertFalse(service.buscarAtual(ambiente.ana).isPresent());
    }

    @Test public void calcular_oitoHorasComIntervalo_descontaUmaHora() {
        ponto(ENTRADA, "2026-09-30T08:00:00-03:00", "2026-09-30");
        ponto(INICIO_INTERVALO, "2026-09-30T12:00:00-03:00", "2026-09-30");
        ponto(FIM_INTERVALO, "2026-09-30T13:00:00-03:00", "2026-09-30");
        ponto(SAIDA, "2026-09-30T17:00:00-03:00", "2026-09-30");
        Jornada jornada = service.buscarAtual(ambiente.ana).get();
        assertEquals(8 * 3_600_000L, jornada.getTrabalhadoMillis());
        assertEquals(3_600_000L, jornada.getIntervaloMillis());
        assertEquals("Encerrada", jornada.getStatus());
        assertFalse(jornada.isAberta());
        assertEquals(4, jornada.getRegistros().size());
    }

    @Test public void calcular_quatroHorasSemIntervalo_naoDescontaTempo() {
        ponto(ENTRADA, "2026-09-30T08:00:00-03:00", "2026-09-30");
        ponto(SAIDA, "2026-09-30T12:00:00-03:00", "2026-09-30");
        Jornada jornada = service.buscarAtual(ambiente.ana).get();
        assertEquals(4 * 3_600_000L, jornada.getTrabalhadoMillis());
        assertEquals(0, jornada.getIntervaloMillis());
    }

    @Test public void calcular_jornadaNoturna_preservaDiaDeEntrada() {
        ponto(ENTRADA, "2026-09-30T22:00:00-03:00", "2026-09-30");
        ponto(SAIDA, "2026-10-01T02:00:00-03:00", "2026-09-30");
        ambiente.relogio.em("2026-10-01T03:00:00-03:00");
        Jornada jornada = service.buscarHistorico(ambiente.ana).get(0);
        assertEquals(LocalDate.of(2026, 9, 30), jornada.getData());
        assertEquals(4 * 3_600_000L, jornada.getTrabalhadoMillis());
        assertFalse(service.buscarAtual(ambiente.ana).isPresent());
    }

    @Test public void calcular_jornadaAberta_acumulaTempoAteAgora() {
        ponto(ENTRADA, "2026-09-30T08:00:00-03:00", "2026-09-30");
        ambiente.relogio.em("2026-09-30T10:30:00-03:00");
        Jornada jornada = service.buscarAtual(ambiente.ana).get();
        assertEquals(150 * 60_000L, jornada.getTrabalhadoMillis());
        assertEquals("Trabalhando", jornada.getStatus());
        assertTrue(jornada.isAberta());
    }

    @Test public void calcular_intervaloAberto_congelaTrabalhoEAcumulaIntervalo() {
        ponto(ENTRADA, "2026-09-30T08:00:00-03:00", "2026-09-30");
        ponto(INICIO_INTERVALO, "2026-09-30T12:00:00-03:00", "2026-09-30");
        ambiente.relogio.em("2026-09-30T13:30:00-03:00");
        Jornada jornada = service.buscarAtual(ambiente.ana).get();
        assertEquals(4 * 3_600_000L, jornada.getTrabalhadoMillis());
        assertEquals(90 * 60_000L, jornada.getIntervaloMillis());
        assertEquals("Em intervalo", jornada.getStatus());
    }

    @Test public void calcular_aposRetornarDoIntervalo_retomaContagem() {
        ponto(ENTRADA, "2026-09-30T08:00:00-03:00", "2026-09-30");
        ponto(INICIO_INTERVALO, "2026-09-30T12:00:00-03:00", "2026-09-30");
        ponto(FIM_INTERVALO, "2026-09-30T13:00:00-03:00", "2026-09-30");
        ambiente.relogio.em("2026-09-30T15:00:00-03:00");
        assertEquals(6 * 3_600_000L, service.buscarAtual(ambiente.ana).get().getTrabalhadoMillis());
        assertEquals("Trabalhando", service.buscarAtual(ambiente.ana).get().getStatus());
    }

    @Test public void buscarAtual_abertaOntem_retornaJornadaAnterior() {
        ponto(ENTRADA, "2026-09-30T22:00:00-03:00", "2026-09-30");
        ambiente.relogio.em("2026-10-01T02:00:00-03:00");
        Jornada jornada = service.buscarAtual(ambiente.ana).get();
        assertEquals(LocalDate.of(2026, 9, 30), jornada.getData());
        assertEquals(4 * 3_600_000L, jornada.getTrabalhadoMillis());
    }

    @Test public void buscarHistorico_variosDiasEUsuarios_ordenaEIsolaContas() {
        ponto(ENTRADA, "2026-09-29T08:00:00-03:00", "2026-09-29");
        ponto(SAIDA, "2026-09-29T12:00:00-03:00", "2026-09-29");
        ponto(ENTRADA, "2026-09-30T08:00:00-03:00", "2026-09-30");
        ambiente.pontos.dados.add(new RegistroPonto(4, ambiente.bia, ENTRADA, ambiente.relogio.millis(), LocalDate.of(2026, 9, 30)));
        List<Jornada> historico = service.buscarHistorico(ambiente.ana);
        assertEquals(2, historico.size());
        assertEquals(LocalDate.of(2026, 9, 30), historico.get(0).getData());
        assertEquals(LocalDate.of(2026, 9, 29), historico.get(1).getData());
        assertEquals(1, historico.get(0).getRegistros().size());
        assertEquals(2, historico.get(1).getRegistros().size());
        assertEquals(1, service.buscarHistorico(ambiente.bia).size());
    }

    @Test public void calcular_relogioAntesDoUltimoPonto_naoGeraTempoNegativo() {
        ponto(ENTRADA, "2026-09-30T08:00:00-03:00", "2026-09-30");
        ambiente.relogio.em("2026-09-30T07:00:00-03:00");
        assertEquals(0, service.buscarAtual(ambiente.ana).get().getTrabalhadoMillis());
    }

    private void ponto(TipoPonto tipo, String instante, String dataJornada) {
        ambiente.pontos.dados.add(new RegistroPonto(ambiente.pontos.dados.size() + 1L, ambiente.ana, tipo,
                OffsetDateTime.parse(instante).toInstant().toEpochMilli(), LocalDate.parse(dataJornada)));
    }
}
