package br.com.bateai.service;

import br.com.bateai.model.RegistroPonto;
import br.com.bateai.model.Resultado;
import br.com.bateai.model.TipoPonto;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static br.com.bateai.model.TipoPonto.*;
import static org.junit.Assert.*;

public class PontoServiceTest {
    private AmbientePontos ambiente;
    private PontoService service;

    @Before public void preparar() {
        ambiente = new AmbientePontos();
        service = new PontoService(ambiente.usuarios, ambiente.pontos, ambiente.relogio);
    }

    @Test public void registrar_sequenciaComIntervalo_preservaTodosOsPontos() {
        assertEquals(Collections.singletonList(ENTRADA), service.acoesDisponiveis(ambiente.ana));
        registrar(ENTRADA);
        assertEquals(Arrays.asList(INICIO_INTERVALO, SAIDA), service.acoesDisponiveis(ambiente.ana));
        registrar(INICIO_INTERVALO);
        assertEquals(Collections.singletonList(FIM_INTERVALO), service.acoesDisponiveis(ambiente.ana));
        registrar(FIM_INTERVALO);
        assertEquals(Collections.singletonList(SAIDA), service.acoesDisponiveis(ambiente.ana));
        registrar(SAIDA);
        assertTrue(service.acoesDisponiveis(ambiente.ana).isEmpty());
        assertEquals(4, ambiente.pontos.listar(ambiente.ana).size());
    }

    @Test public void registrar_semIntervalo_permiteSaidaDireta() {
        registrar(ENTRADA);
        registrar(SAIDA);
        assertEquals(2, ambiente.pontos.listar(ambiente.ana).size());
    }

    @Test public void registrar_semEntrada_rejeitaOutrosTipos() {
        rejeitar(SAIDA); rejeitar(INICIO_INTERVALO); rejeitar(FIM_INTERVALO);
        assertTrue(ambiente.pontos.dados.isEmpty());
    }

    @Test public void registrar_transicoesInvalidas_naoAlteraSequencia() {
        registrar(ENTRADA);
        rejeitar(ENTRADA); rejeitar(FIM_INTERVALO);
        registrar(INICIO_INTERVALO);
        rejeitar(ENTRADA); rejeitar(INICIO_INTERVALO); rejeitar(SAIDA);
        registrar(FIM_INTERVALO);
        rejeitar(ENTRADA); rejeitar(INICIO_INTERVALO); rejeitar(FIM_INTERVALO);
        registrar(SAIDA);
        for (TipoPonto tipo : TipoPonto.values()) rejeitar(tipo);
        assertEquals(4, ambiente.pontos.dados.size());
    }

    @Test public void registrar_jornadaOntemAberta_bloqueiaEntradaEPreservaData() {
        ambiente.relogio.em("2026-09-30T22:00:00-03:00");
        registrar(ENTRADA);
        ambiente.relogio.em("2026-10-01T01:00:00-03:00");
        rejeitar(ENTRADA);
        assertEquals(LocalDate.of(2026, 9, 30), registrar(INICIO_INTERVALO).getDataJornada());
        assertEquals(LocalDate.of(2026, 9, 30), registrar(FIM_INTERVALO).getDataJornada());
        assertEquals(LocalDate.of(2026, 9, 30), registrar(SAIDA).getDataJornada());
        assertEquals(LocalDate.of(2026, 10, 1), registrar(ENTRADA).getDataJornada());
        assertEquals(5, ambiente.pontos.dados.size());
    }

    @Test public void registrar_intervaloAtravessaMeiaNoite_exigeFinalizacao() {
        registrar(ENTRADA);
        registrar(INICIO_INTERVALO);
        ambiente.relogio.em("2026-10-01T01:00:00-03:00");
        assertEquals(Collections.singletonList(FIM_INTERVALO), service.acoesDisponiveis(ambiente.ana));
        rejeitar(SAIDA); rejeitar(ENTRADA);
        assertEquals(LocalDate.of(2026, 9, 30), registrar(FIM_INTERVALO).getDataJornada());
    }

    @Test public void registrar_relogioRetrocedeu_rejeitaSemPersistir() {
        registrar(ENTRADA);
        ambiente.relogio.em("2026-09-30T07:59:59-03:00");
        Resultado<RegistroPonto> resultado = service.registrar(ambiente.ana, SAIDA);
        assertFalse(resultado.isSucesso());
        assertTrue(resultado.getMensagem().contains("relógio"));
        assertEquals(1, ambiente.pontos.dados.size());
    }

    @Test public void registrar_usuarioInexistenteOuTipoAusente_rejeita() {
        assertFalse(service.registrar(999, ENTRADA).isSucesso());
        assertTrue(service.acoesDisponiveis(999).isEmpty());
        assertFalse(service.registrar(ambiente.ana, null).isSucesso());
        assertTrue(ambiente.pontos.dados.isEmpty());
    }

    @Test public void registrar_duasContas_isolaEstados() {
        registrar(ENTRADA);
        registrar(INICIO_INTERVALO);
        assertEquals(Collections.singletonList(ENTRADA), service.acoesDisponiveis(ambiente.bia));
        assertTrue(service.registrar(ambiente.bia, ENTRADA).isSucesso());
        assertEquals(2, ambiente.pontos.listar(ambiente.ana).size());
        assertEquals(1, ambiente.pontos.listar(ambiente.bia).size());
    }

    @Test public void registrar_falhaAoSalvar_retornaErroEPreservaEstado() {
        ambiente.pontos.falhar = true;
        assertFalse(service.registrar(ambiente.ana, ENTRADA).isSucesso());
        assertTrue(ambiente.pontos.dados.isEmpty());
        assertEquals(Collections.singletonList(ENTRADA), service.acoesDisponiveis(ambiente.ana));
    }

    @Test public void registrar_entradasConcorrentes_gravaUmaUnicaEntrada() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Resultado<RegistroPonto>> entrada = () -> service.registrar(ambiente.ana, ENTRADA);
            java.util.List<Future<Resultado<RegistroPonto>>> resultados = executor.invokeAll(Arrays.asList(entrada, entrada));
            long sucessos = 0;
            for (Future<Resultado<RegistroPonto>> resultado : resultados) if (resultado.get().isSucesso()) sucessos++;
            assertEquals(1, sucessos);
            assertEquals(1, ambiente.pontos.dados.size());
        } finally { executor.shutdownNow(); }
    }

    private RegistroPonto registrar(TipoPonto tipo) {
        Resultado<RegistroPonto> resultado = service.registrar(ambiente.ana, tipo);
        assertTrue(resultado.getMensagem(), resultado.isSucesso());
        return resultado.getValor();
    }
    private void rejeitar(TipoPonto tipo) { assertFalse("Deveria rejeitar " + tipo, service.registrar(ambiente.ana, tipo).isSucesso()); }
}
