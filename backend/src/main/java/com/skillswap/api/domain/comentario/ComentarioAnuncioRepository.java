package com.skillswap.api.domain.comentario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ComentarioAnuncioRepository extends JpaRepository<ComentarioAnuncio, Long> {
    @Query("select c from ComentarioAnuncio c where c.anuncio.id = :anuncioId and c.moderadaOculta = false")
    @EntityGraph(attributePaths = {"autor", "respostaA", "respostaA.autor"})
    Page<ComentarioAnuncio> listarVisiveis(@Param("anuncioId") Long anuncioId, Pageable pageable);

    @EntityGraph(attributePaths = {"anuncio", "autor", "respostaA", "respostaA.autor"})
    Optional<ComentarioAnuncio> findByIdAndAnuncioIdAndModeradaOcultaFalse(Long id, Long anuncioId);

    Optional<ComentarioAnuncio> findByIdAndModeradaOcultaFalse(Long id);
}
