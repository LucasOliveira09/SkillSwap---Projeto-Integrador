package com.skillswap.api.domain.portfolio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PublicacaoPortfolioRepository extends JpaRepository<PublicacaoPortfolio, Long> {
    @EntityGraph(attributePaths = {"usuario", "anuncio"})
    Page<PublicacaoPortfolio> findByUsuarioId(Long usuarioId, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "anuncio"})
    Optional<PublicacaoPortfolio> findById(Long id);
}
