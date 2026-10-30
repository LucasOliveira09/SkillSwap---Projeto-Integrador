package com.skillswap.api.domain.mensagem;

import com.skillswap.api.domain.acordo.Acordo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
    String NAO_LIDA_PROPOSTA = """
            m.remetente.id <> :usuario and (
              (case when m.proposta.proponente.id=:usuario then m.proposta.ultimaLeituraProponenteEm else m.proposta.ultimaLeituraAnuncianteEm end) is null
              or m.enviadoEm > (case when m.proposta.proponente.id=:usuario then m.proposta.ultimaLeituraProponenteEm else m.proposta.ultimaLeituraAnuncianteEm end)
              or (m.enviadoEm = (case when m.proposta.proponente.id=:usuario then m.proposta.ultimaLeituraProponenteEm else m.proposta.ultimaLeituraAnuncianteEm end)
                and m.id > (case when m.proposta.proponente.id=:usuario then m.proposta.ultimaLidaProponente else m.proposta.ultimaLidaAnunciante end)))
            """;

    String NAO_LIDA_DIRETA = """
            m.remetente.id <> :usuario and (
              (case when m.conversa.participanteMenor.id=:usuario then m.conversa.ultimaLeituraMenorEm else m.conversa.ultimaLeituraMaiorEm end) is null
              or m.enviadoEm > (case when m.conversa.participanteMenor.id=:usuario then m.conversa.ultimaLeituraMenorEm else m.conversa.ultimaLeituraMaiorEm end)
              or (m.enviadoEm = (case when m.conversa.participanteMenor.id=:usuario then m.conversa.ultimaLeituraMenorEm else m.conversa.ultimaLeituraMaiorEm end)
                and m.id > (case when m.conversa.participanteMenor.id=:usuario then m.conversa.ultimaLidaMenor else m.conversa.ultimaLidaMaior end)))
            """;

    @Query(value = """
            select count(*) from mensagens m
            join conversas c on c.id = m.conversa_id
            left join propostas p on p.id = m.proposta_id
            where (c.participante_menor_id = :usuario or c.participante_maior_id = :usuario)
              and m.remetente_id <> :usuario
              and ((p.id is null and (
                    (case when c.participante_menor_id = :usuario then c.ultima_leitura_menor_em else c.ultima_leitura_maior_em end) is null
                    or m.enviado_em > case when c.participante_menor_id = :usuario then c.ultima_leitura_menor_em else c.ultima_leitura_maior_em end
                    or (m.enviado_em = case when c.participante_menor_id = :usuario then c.ultima_leitura_menor_em else c.ultima_leitura_maior_em end
                        and m.id > case when c.participante_menor_id = :usuario then c.ultima_lida_menor else c.ultima_lida_maior end)))
                or (p.id is not null and (
                    (case when p.proponente_id = :usuario then p.ultima_leitura_proponente_em else p.ultima_leitura_anunciante_em end) is null
                    or m.enviado_em > case when p.proponente_id = :usuario then p.ultima_leitura_proponente_em else p.ultima_leitura_anunciante_em end
                    or (m.enviado_em = case when p.proponente_id = :usuario then p.ultima_leitura_proponente_em else p.ultima_leitura_anunciante_em end
                        and m.id > case when p.proponente_id = :usuario then p.ultima_lida_proponente else p.ultima_lida_anunciante end))))
            """, nativeQuery = true)
    long contarNaoLidas(@Param("usuario") Long usuario);

    @EntityGraph(attributePaths = {"remetente", "anuncioContexto", "proposta", "proposta.anuncio", "proposta.anuncio.usuario", "proposta.proponente", "acordo"})
    Page<Mensagem> findByConversaId(Long conversaId, Pageable pageable);

    @Query("select m from Mensagem m join fetch m.remetente left join fetch m.anuncioContexto left join fetch m.proposta p left join fetch p.anuncio left join fetch p.anuncio.usuario left join fetch p.proponente left join fetch m.acordo where m.conversa.id in :ids and not exists (select n.id from Mensagem n where n.conversa.id=m.conversa.id and (n.enviadoEm>m.enviadoEm or (n.enviadoEm=m.enviadoEm and n.id>m.id)))")
    List<Mensagem> ultimas(@Param("ids") List<Long> ids);

    interface Contagem { Long getConversaId(); Long getQuantidade(); }
    @Query(value = """
            select m.conversa_id as conversaId, count(*) as quantidade
            from mensagens m
            join conversas c on c.id = m.conversa_id
            left join propostas p on p.id = m.proposta_id
            where m.conversa_id in (:ids) and m.remetente_id <> :usuario
              and ((p.id is null and (
                    (case when c.participante_menor_id = :usuario then c.ultima_leitura_menor_em else c.ultima_leitura_maior_em end) is null
                    or m.enviado_em > case when c.participante_menor_id = :usuario then c.ultima_leitura_menor_em else c.ultima_leitura_maior_em end
                    or (m.enviado_em = case when c.participante_menor_id = :usuario then c.ultima_leitura_menor_em else c.ultima_leitura_maior_em end
                        and m.id > case when c.participante_menor_id = :usuario then c.ultima_lida_menor else c.ultima_lida_maior end)))
                or (p.id is not null and (
                    (case when p.proponente_id = :usuario then p.ultima_leitura_proponente_em else p.ultima_leitura_anunciante_em end) is null
                    or m.enviado_em > case when p.proponente_id = :usuario then p.ultima_leitura_proponente_em else p.ultima_leitura_anunciante_em end
                    or (m.enviado_em = case when p.proponente_id = :usuario then p.ultima_leitura_proponente_em else p.ultima_leitura_anunciante_em end
                        and m.id > case when p.proponente_id = :usuario then p.ultima_lida_proponente else p.ultima_lida_anunciante end))))
            group by m.conversa_id
            """, nativeQuery = true)
    List<Contagem> contarPorConversa(@Param("ids") List<Long> ids, @Param("usuario") Long usuario);

    @EntityGraph(attributePaths = {"remetente", "proposta", "anuncioContexto", "conversa"})
    Optional<Mensagem> findByIdAndConversaId(Long id, Long conversaId);

    @Query("select m from Mensagem m left join fetch m.proposta join fetch m.remetente where m.conversa.id = :conversaId and m.proposta is not null and (m.enviadoEm < :data or (m.enviadoEm = :data and m.id <= :id)) and not exists (select n.id from Mensagem n where n.conversa.id=m.conversa.id and n.proposta=m.proposta and (n.enviadoEm < :data or (n.enviadoEm = :data and n.id <= :id)) and (n.enviadoEm > m.enviadoEm or (n.enviadoEm=m.enviadoEm and n.id>m.id)))")
    List<Mensagem> ultimasPorPropostaAte(@Param("conversaId") Long conversaId, @Param("data") LocalDateTime data, @Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE mensagens SET conversa_id = (SELECT p.conversa_id FROM propostas p WHERE p.id = mensagens.proposta_id), anuncio_contexto_id = (SELECT p.anuncio_id FROM propostas p WHERE p.id = mensagens.proposta_id) WHERE proposta_id IS NOT NULL AND (conversa_id IS NULL OR anuncio_contexto_id IS NULL)", nativeQuery = true)
    int reconciliarConversasLegadas();

    @Modifying
    @Query(value = "UPDATE mensagens SET proposta_id = (SELECT a.proposta_id FROM acordos a WHERE a.id = mensagens.acordo_id), conversa_id = (SELECT p.conversa_id FROM acordos a JOIN propostas p ON p.id = a.proposta_id WHERE a.id = mensagens.acordo_id), anuncio_contexto_id = (SELECT p.anuncio_id FROM acordos a JOIN propostas p ON p.id = a.proposta_id WHERE a.id = mensagens.acordo_id) WHERE acordo_id IS NOT NULL AND (proposta_id IS NULL OR conversa_id IS NULL)", nativeQuery = true)
    int reconciliarAcordosLegados();

    @Modifying
    @Query("update Mensagem m set m.acordo = :acordo where m.proposta.id = :propostaId and m.acordo is null")
    void associarAcordo(@Param("propostaId") Long propostaId, @Param("acordo") Acordo acordo);

    @EntityGraph(attributePaths = {"remetente", "acordo"})
    List<Mensagem> findByAcordoIdOrderByEnviadoEmAsc(Long acordoId);

    @EntityGraph(attributePaths = {"remetente", "acordo"})
    Page<Mensagem> findByAcordoId(Long acordoId, Pageable pageable);

    @EntityGraph(attributePaths = {"remetente", "acordo"})
    Optional<Mensagem> findByIdAndAcordoId(Long id, Long acordoId);
}
