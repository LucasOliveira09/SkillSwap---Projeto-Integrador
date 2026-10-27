package com.skillswap.api.domain.avaliacao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.skillswap.api.domain.avaliacao.dto.ResumoAvaliacaoAnuncioDTO;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    @EntityGraph(attributePaths = {"avaliador", "avaliado", "acordo"})
    List<Avaliacao> findByAvaliadoId(Long avaliadoId);

    long countByAvaliadoId(Long avaliadoId);

    boolean existsByAcordoIdAndAvaliadorId(Long acordoId, Long avaliadorId);

    @EntityGraph(attributePaths = {"avaliador", "avaliado", "acordo"})
    List<Avaliacao> findByAcordoId(Long acordoId);

    @EntityGraph(attributePaths = {"avaliador", "avaliado", "acordo"})
    Optional<Avaliacao> findByIdAndAcordoId(Long id, Long acordoId);

    @Query("select new com.skillswap.api.domain.avaliacao.dto.ResumoAvaliacaoAnuncioDTO(a.acordo.proposta.anuncio.id, avg(a.notaAnuncio), count(a)) " +
            "from Avaliacao a where a.acordo.proposta.anuncio.id in :anuncioIds and a.notaAnuncio is not null " +
            "group by a.acordo.proposta.anuncio.id")
    List<ResumoAvaliacaoAnuncioDTO> resumirAnunciosAvaliados(@Param("anuncioIds") Collection<Long> anuncioIds);
}
