package br.com.bateai.repository;

import br.com.bateai.model.Usuario;
import java.util.Optional;

public interface UsuarioRepository {
    Optional<Usuario> buscarPorId(long id);
    Optional<Usuario> buscarPorEmail(String email);
    Usuario inserir(Usuario usuario);
}
