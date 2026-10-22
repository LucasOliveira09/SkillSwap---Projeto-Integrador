package com.skillswap.api.domain.anuncio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {

    @EntityGraph(attributePaths = {"usuario"})
    Page<Anuncio> findByStatus(StatusAnuncio status, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario"})
    List<Anuncio> findByUsuarioId(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario"})
    Optional<Anuncio> findById(Long id);

    @EntityGraph(attributePaths = {"usuario"})
    @Query("SELECT a FROM Anuncio a WHERE a.status = :status " +
           "AND (:categoria IS NULL OR a.categoria = :categoria) " +
           "AND (:cidade IS NULL OR LOWER(a.cidade) LIKE LOWER(CONCAT('%', :cidade, '%')))")
    Page<Anuncio> findByStatusAndFilters(
            @Param("status") StatusAnuncio status,
            @Param("categoria") CategoriaAnuncio categoria,
            @Param("cidade") String cidade,
            Pageable pageable);

    boolean existsByCodigoLegado(String codigo);

    @EntityGraph(attributePaths = {"usuario"})
    @Query("SELECT a FROM Anuncio a WHERE a.status = :status " +
           "AND (:categoria IS NULL OR a.categoria = :categoria) " +
           "AND (:cidade IS NULL OR LOWER(a.cidade) LIKE LOWER(CONCAT('%', :cidade, '%'))) " +
           "AND a.textoBusca LIKE :busca ESCAPE '!'")
    Page<Anuncio> pesquisar(@Param("status") StatusAnuncio status,
        @Param("categoria") CategoriaAnuncio categoria, @Param("cidade") String cidade,
        @Param("busca") String busca, Pageable pageable);
}
