package br.com.bateai.repository;

import br.com.bateai.model.RegistroPonto;
import java.util.List;
import java.util.function.Supplier;

public interface PontoRepository {
    /** Retorna somente os pontos do usuário, por timestamp e id crescentes. */
    List<RegistroPonto> listar(long usuarioId);
    RegistroPonto inserir(RegistroPonto registro);
    /** Serializa leitura, validação e escrita; desfaz todas as escritas se a operação lançar uma exceção. */
    <T> T emTransacao(Supplier<T> operacao);
}
