package com.skillswap.api.domain.portfolio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PublicacaoPortfolioRepository extends JpaRepository<PublicacaoPortfolio, Long> {
    @EntityGraph(attributePaths = {"usuario", "anuncio"})
    Page<PublicacaoPortfolio> findByUsuarioId(Long usuarioId, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "anuncio"})
    Page<PublicacaoPortfolio> findByUsuarioIdAndModeradaOcultaFalse(Long usuarioId, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "anuncio"})
    Optional<PublicacaoPortfolio> findByIdAndModeradaOcultaFalse(Long id);

    @EntityGraph(attributePaths = {"usuario", "anuncio"})
    Optional<PublicacaoPortfolio> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PublicacaoPortfolio p join fetch p.usuario where p.id = :id and p.moderadaOculta = false")
    Optional<PublicacaoPortfolio> findVisivelPorIdParaAtualizar(@Param("id") Long id);

    @Query("select distinct foto from PublicacaoPortfolio p join p.fotos foto")
    java.util.List<String> listarFotosReferenciadas();

    @Query("select case when count(p) > 0 then true else false end " +
            "from PublicacaoPortfolio p join p.fotos foto " +
            "where foto = :fotoUrl and p.moderadaOculta = false and p.usuario.perfilPublico = true")
    boolean existeFotoPublica(@Param("fotoUrl") String fotoUrl);
}
