package br.com.bateai.service;

import br.com.bateai.model.RegistroPonto;
import br.com.bateai.model.Resultado;
import br.com.bateai.model.TipoPonto;
import br.com.bateai.repository.PersistenciaException;
import br.com.bateai.repository.PontoRepository;
import br.com.bateai.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PontoService {
    private final UsuarioRepository usuarios;
    private final PontoRepository pontos;
    private final Clock relogio;

    public PontoService(UsuarioRepository usuarios, PontoRepository pontos, Clock relogio) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.pontos = Objects.requireNonNull(pontos);
        this.relogio = Objects.requireNonNull(relogio);
    }

    public Resultado<RegistroPonto> registrar(long usuarioId, TipoPonto tipo) {
        if (tipo == null) return Resultado.erro("Selecione uma ação para registrar o ponto.");
        try {
            return pontos.emTransacao(() -> registrarNaTransacao(usuarioId, tipo));
        } catch (PersistenciaException erro) {
            return Resultado.erro("Não foi possível salvar o ponto. Atualize a tela e tente novamente.");
        }
    }

    private Resultado<RegistroPonto> registrarNaTransacao(long usuarioId, TipoPonto tipo) {
        if (!usuarios.buscarPorId(usuarioId).isPresent()) return Resultado.erro("Usuário não encontrado. Entre novamente.");
        List<RegistroPonto> registros = pontos.listar(usuarioId);
        long agora = relogio.millis();
        LocalDate hoje = Instant.ofEpochMilli(agora).atZone(relogio.getZone()).toLocalDate();
        if (!registros.isEmpty() && agora < registros.get(registros.size() - 1).getTimestamp()) {
            return Resultado.erro("O relógio está anterior ao último ponto. Corrija a hora do dispositivo e tente novamente.");
        }
        if (!acoesPara(registros, hoje).contains(tipo)) {
            return Resultado.erro(mensagemTransicaoInvalida(registros));
        }
        LocalDate dataJornada = tipo == TipoPonto.ENTRADA
                ? hoje : registros.get(registros.size() - 1).getDataJornada();
        RegistroPonto salvo = pontos.inserir(new RegistroPonto(0, usuarioId, tipo, agora, dataJornada));
        return Resultado.sucesso(salvo, "Ponto registrado: " + tipo.getDescricao() + ".");
    }

    public List<TipoPonto> acoesDisponiveis(long usuarioId) {
        if (!usuarios.buscarPorId(usuarioId).isPresent()) return Collections.emptyList();
        return acoesPara(pontos.listar(usuarioId), LocalDate.now(relogio));
    }

    private static List<TipoPonto> acoesPara(List<RegistroPonto> registros, LocalDate hoje) {
        if (!registros.isEmpty()) {
            TipoPonto ultimo = registros.get(registros.size() - 1).getTipo();
            switch (ultimo) {
                case ENTRADA: return Collections.unmodifiableList(Arrays.asList(TipoPonto.INICIO_INTERVALO, TipoPonto.SAIDA));
                case INICIO_INTERVALO: return Collections.singletonList(TipoPonto.FIM_INTERVALO);
                case FIM_INTERVALO: return Collections.singletonList(TipoPonto.SAIDA);
                case SAIDA: break;
            }
        }
        for (RegistroPonto registro : registros) {
            if (registro.getTipo() == TipoPonto.ENTRADA && registro.getDataJornada().equals(hoje)) {
                return Collections.emptyList();
            }
        }
        return Collections.singletonList(TipoPonto.ENTRADA);
    }

    private static String mensagemTransicaoInvalida(List<RegistroPonto> registros) {
        if (registros.isEmpty()) return "Você precisa registrar uma entrada primeiro.";
        switch (registros.get(registros.size() - 1).getTipo()) {
            case ENTRADA: return "Sua jornada já está aberta. Inicie o intervalo ou registre a saída.";
            case INICIO_INTERVALO: return "Finalize o intervalo antes de registrar outro ponto.";
            case FIM_INTERVALO: return "Seu intervalo já terminou. Registre a saída para encerrar a jornada.";
            default: return "Sua jornada já foi encerrada. É permitida uma entrada por dia.";
        }
    }
}
