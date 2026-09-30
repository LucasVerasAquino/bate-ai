package br.com.bateai.repository;

public final class EmailDuplicadoException extends PersistenciaException {
    public EmailDuplicadoException(String mensagem) { super(mensagem); }
    public EmailDuplicadoException(String mensagem, Throwable causa) { super(mensagem, causa); }
}
