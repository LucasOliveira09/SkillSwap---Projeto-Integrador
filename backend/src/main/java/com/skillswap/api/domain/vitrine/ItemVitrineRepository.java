package com.skillswap.api.domain.vitrine;

import com.skillswap.api.domain.vitrine.ItemVitrine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemVitrineRepository extends JpaRepository<ItemVitrine, Integer> {
    boolean existsByCodigo(String codigo);
    List<ItemVitrine> findAllByOrderByOrdemAscIdAsc();
}
