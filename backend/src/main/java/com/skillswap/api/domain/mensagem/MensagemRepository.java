package com.skillswap.api.domain.mensagem;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    String NAO_LIDA = """
            m.remetente.id <> :usuario and (
              (case when m.proposta.proponente.id=:usuario then m.proposta.ultimaLeituraProponenteEm else m.proposta.ultimaLeituraAnuncianteEm end) is null
              or m.enviadoEm > (case when m.proposta.proponente.id=:usuario then m.proposta.ultimaLeituraProponenteEm else m.proposta.ultimaLeituraAnuncianteEm end)
              or (m.enviadoEm = (case when m.proposta.proponente.id=:usuario then m.proposta.ultimaLeituraProponenteEm else m.proposta.ultimaLeituraAnuncianteEm end)
                and m.id > (case when m.proposta.proponente.id=:usuario then m.proposta.ultimaLidaProponente else m.proposta.ultimaLidaAnunciante end)))
            """;

    @Query("select count(m) from Mensagem m where (m.proposta.anuncio.usuario.id = :usuario or m.proposta.proponente.id = :usuario) and " + NAO_LIDA)
    long contarNaoLidas(@Param("usuario") Long usuario);

    @EntityGraph(attributePaths = {"remetente"})
    Page<Mensagem> findByPropostaId(Long propostaId, Pageable pageable);

    @Query("select m from Mensagem m join fetch m.remetente where m.proposta.id in :ids and not exists (select n.id from Mensagem n where n.proposta.id=m.proposta.id and (n.enviadoEm>m.enviadoEm or (n.enviadoEm=m.enviadoEm and n.id>m.id)))")
    List<Mensagem> ultimas(@Param("ids") List<Long> ids);

    interface Contagem { Long getPropostaId(); Long getQuantidade(); }
    @Query("select m.proposta.id as propostaId, count(m) as quantidade from Mensagem m where m.proposta.id in :ids and " + NAO_LIDA + " group by m.proposta.id")
    List<Contagem> contarPorConversa(@Param("ids") List<Long> ids, @Param("usuario") Long usuario);

    Optional<Mensagem> findByIdAndPropostaId(Long id, Long propostaId);

    @Modifying
    @Query(value = "UPDATE mensagens SET proposta_id = (SELECT a.proposta_id FROM acordos a WHERE a.id = mensagens.acordo_id) WHERE proposta_id IS NULL AND acordo_id IS NOT NULL", nativeQuery = true)
    int reconciliarLegadas();

    @Modifying
    @Query("update Mensagem m set m.acordo = :acordo where m.proposta.id = :propostaId and m.acordo is null")
    void associarAcordo(@Param("propostaId") Long propostaId, @Param("acordo") com.skillswap.api.domain.acordo.Acordo acordo);

    @EntityGraph(attributePaths = {"remetente", "acordo"})
    List<Mensagem> findByAcordoIdOrderByEnviadoEmAsc(Long acordoId);

    @EntityGraph(attributePaths = {"remetente", "acordo"})
    Page<Mensagem> findByAcordoId(Long acordoId, Pageable pageable);

    @EntityGraph(attributePaths = {"remetente", "acordo"})
    Optional<Mensagem> findByIdAndAcordoId(Long id, Long acordoId);
}
