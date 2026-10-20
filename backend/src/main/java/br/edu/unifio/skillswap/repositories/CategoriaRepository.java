package br.edu.unifio.skillswap.repositories;

import br.edu.unifio.skillswap.entities.Categoria;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
    Optional<Categoria> findByCodigo(String codigo);
}
