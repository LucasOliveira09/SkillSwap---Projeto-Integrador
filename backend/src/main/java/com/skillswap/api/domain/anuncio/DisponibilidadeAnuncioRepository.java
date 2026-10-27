package com.skillswap.api.domain.anuncio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DisponibilidadeAnuncioRepository extends JpaRepository<DisponibilidadeAnuncio, Long> {
    @Query("select d from DisponibilidadeAnuncio d where d.anuncio.id in :anuncioIds")
    List<DisponibilidadeAnuncio> buscarPorAnuncios(@Param("anuncioIds") Collection<Long> anuncioIds);
}
