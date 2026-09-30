package br.com.bateai.model;

import java.util.Objects;

public final class Usuario {
    private final long id;
    private final String nome;
    private final String email;
    private final byte[] senhaHash;
    private final byte[] salt;
    private final long criadoEm;

    public Usuario(long id, String nome, String email, byte[] senhaHash, byte[] salt, long criadoEm) {
        this.id = id;
        this.nome = Objects.requireNonNull(nome);
        this.email = Objects.requireNonNull(email);
        this.senhaHash = senhaHash.clone();
        this.salt = salt.clone();
        this.criadoEm = criadoEm;
    }

    public long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public byte[] getSenhaHash() { return senhaHash.clone(); }
    public byte[] getSalt() { return salt.clone(); }
    public long getCriadoEm() { return criadoEm; }
}
