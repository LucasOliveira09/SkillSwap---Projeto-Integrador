package br.edu.unifio.skillswap.repositories;

import br.edu.unifio.skillswap.entities.ItemVitrine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemVitrineRepository extends JpaRepository<ItemVitrine, Integer> {
    boolean existsByCodigo(String codigo);
    List<ItemVitrine> findAllByOrderByOrdemAscIdAsc();
}
