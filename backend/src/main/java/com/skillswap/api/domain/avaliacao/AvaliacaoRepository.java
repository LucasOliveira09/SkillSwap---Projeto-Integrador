package com.skillswap.api.domain.avaliacao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import com.skillswap.api.domain.avaliacao.dto.ResumoAvaliacaoAnuncioDTO;
import com.skillswap.api.domain.avaliacao.dto.ResumoAvaliacaoProfissionalDTO;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    @EntityGraph(attributePaths = {"avaliador", "avaliado", "acordo"})
    List<Avaliacao> findByAvaliadoId(Long avaliadoId);

    @EntityGraph(attributePaths = {"avaliador", "avaliado", "acordo"})
    List<Avaliacao> findByAvaliadoIdAndModeradaOcultaFalse(Long avaliadoId);

    long countByAvaliadoId(Long avaliadoId);

    boolean existsByAcordoIdAndAvaliadorId(Long acordoId, Long avaliadorId);

    @EntityGraph(attributePaths = {"avaliador", "avaliado", "acordo"})
    List<Avaliacao> findByAcordoId(Long acordoId);

    @EntityGraph(attributePaths = {"avaliador", "acordo", "acordo.proposta", "acordo.proposta.anuncio"})
    List<Avaliacao> findByAcordoPropostaAnuncioIdAndNotaAnuncioIsNotNullAndModeradaOcultaFalseOrderByCriadoEmDescIdDesc(Long anuncioId);

    @EntityGraph(attributePaths = {"avaliador", "avaliado", "acordo"})
    Optional<Avaliacao> findByIdAndAcordoId(Long id, Long acordoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Avaliacao a join fetch a.avaliado where a.id = :id and a.acordo.id = :acordoId")
    Optional<Avaliacao> findByIdAndAcordoIdForUpdate(@Param("id") Long id, @Param("acordoId") Long acordoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Avaliacao a where a.id = :id")
    Optional<Avaliacao> findByIdForUpdate(@Param("id") Long id);

    @Query("select new com.skillswap.api.domain.avaliacao.dto.ResumoAvaliacaoAnuncioDTO(a.acordo.proposta.anuncio.id, avg(a.notaAnuncio), count(a)) " +
            "from Avaliacao a where a.acordo.proposta.anuncio.id in :anuncioIds and a.notaAnuncio is not null and a.moderadaOculta = false " +
            "group by a.acordo.proposta.anuncio.id")
    List<ResumoAvaliacaoAnuncioDTO> resumirAnunciosAvaliados(@Param("anuncioIds") Collection<Long> anuncioIds);

    @Query("select new com.skillswap.api.domain.avaliacao.dto.ResumoAvaliacaoProfissionalDTO(a.avaliado.id, avg(a.nota), count(a)) " +
            "from Avaliacao a where a.avaliado.id in :profissionalIds and a.moderadaOculta = false " +
            "group by a.avaliado.id")
    List<ResumoAvaliacaoProfissionalDTO> resumirProfissionais(@Param("profissionalIds") Collection<Long> profissionalIds);
}
