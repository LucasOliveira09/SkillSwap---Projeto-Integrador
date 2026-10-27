package com.skillswap.api.domain.anuncio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {

    @EntityGraph(attributePaths = {"usuario"})
    Page<Anuncio> findByStatus(StatusAnuncio status, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario"})
    List<Anuncio> findByUsuarioId(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario"})
    Page<Anuncio> findByUsuarioIdAndStatusAndExemploFalse(Long usuarioId, StatusAnuncio status, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario"})
    Page<Anuncio> findByUsuarioIdAndExemploFalse(Long usuarioId, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario"})
    Optional<Anuncio> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Anuncio a where a.id = :id")
    Optional<Anuncio> findByIdForUpdate(@Param("id") Long id);

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

    @Query("select a.imagem from Anuncio a where a.imagem is not null")
    List<String> listarImagens();

    @EntityGraph(attributePaths = {"usuario"})
    Optional<Anuncio> findByCodigoLegado(String codigoLegado);

    java.util.List<Anuncio> findTop100ByTextoBuscaIsNull();

    @EntityGraph(attributePaths = {"usuario"})
    @Query("SELECT a FROM Anuncio a WHERE a.status = :status " +
           "AND (:categoria IS NULL OR a.categoria = :categoria) " +
           "AND (:cidade IS NULL OR LOWER(a.cidade) LIKE LOWER(CONCAT('%', :cidade, '%'))) " +
           "AND (:modalidade IS NULL OR a.modalidade = :modalidade) " +
           "AND (:precoMinimo IS NULL OR (a.preco IS NOT NULL AND a.preco >= :precoMinimo)) " +
           "AND (:precoMaximo IS NULL OR (a.preco IS NOT NULL AND a.preco <= :precoMaximo)) " +
           "AND a.textoBusca LIKE :busca ESCAPE '!'")
    Page<Anuncio> pesquisar(@Param("status") StatusAnuncio status,
        @Param("categoria") CategoriaAnuncio categoria, @Param("cidade") String cidade,
        @Param("modalidade") ModalidadeAnuncio modalidade,
        @Param("precoMinimo") BigDecimal precoMinimo, @Param("precoMaximo") BigDecimal precoMaximo,
        @Param("busca") String busca, Pageable pageable);
}
