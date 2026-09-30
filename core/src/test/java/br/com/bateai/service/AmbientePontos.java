package br.com.bateai.service;

import br.com.bateai.model.RegistroPonto;
import br.com.bateai.model.Usuario;
import br.com.bateai.repository.PersistenciaException;
import br.com.bateai.repository.PontoRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class AmbientePontos {
    final Relogio relogio = new Relogio("2026-09-30T08:00:00-03:00");
    final AuthServiceTest.MemoriaUsuarios usuarios = new AuthServiceTest.MemoriaUsuarios();
    final MemoriaPontos pontos = new MemoriaPontos();
    final long ana;
    final long bia;

    AmbientePontos() {
        ana = usuarios.inserir(new Usuario(0, "Ana", "ana@exemplo.com", new byte[32], new byte[16], relogio.millis())).getId();
        bia = usuarios.inserir(new Usuario(0, "Bia", "bia@exemplo.com", new byte[32], new byte[16], relogio.millis())).getId();
    }

    static final class Relogio extends Clock {
        private Instant instante;
        private final ZoneId zona;
        Relogio(String instante) { this(java.time.OffsetDateTime.parse(instante).toInstant(), ZoneId.of("America/Sao_Paulo")); }
        private Relogio(Instant instante, ZoneId zona) { this.instante = instante; this.zona = zona; }
        void em(String instante) { this.instante = java.time.OffsetDateTime.parse(instante).toInstant(); }
        @Override public ZoneId getZone() { return zona; }
        @Override public Clock withZone(ZoneId zona) { return new Relogio(instante, zona); }
        @Override public Instant instant() { return instante; }
    }

    static final class MemoriaPontos implements PontoRepository {
        final List<RegistroPonto> dados = new ArrayList<>();
        boolean falhar;
        boolean emTransacao;
        @Override public synchronized List<RegistroPonto> listar(long id) {
            return dados.stream().filter(r -> r.getUsuarioId() == id)
                    .sorted(Comparator.comparingLong(RegistroPonto::getTimestamp).thenComparingLong(RegistroPonto::getId))
                    .collect(Collectors.toList());
        }
        @Override public synchronized RegistroPonto inserir(RegistroPonto registro) {
            if (!emTransacao) throw new AssertionError("A gravação deve compartilhar a transação com a validação.");
            if (falhar) throw new PersistenciaException("disco indisponível");
            RegistroPonto salvo = new RegistroPonto(dados.size() + 1L, registro.getUsuarioId(), registro.getTipo(), registro.getTimestamp(), registro.getDataJornada());
            dados.add(salvo);
            return salvo;
        }
        @Override public synchronized <T> T emTransacao(Supplier<T> operacao) {
            List<RegistroPonto> antes = new ArrayList<>(dados);
            emTransacao = true;
            try { return operacao.get(); }
            catch (RuntimeException erro) { dados.clear(); dados.addAll(antes); throw erro; }
            finally { emTransacao = false; }
        }
    }
}
