package com.skillswap.api.domain.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CurtidaPublicacaoRepository extends JpaRepository<CurtidaPublicacao, Long> {
    boolean existsByPublicacaoIdAndUsuarioId(Long publicacaoId, Long usuarioId);
    Optional<CurtidaPublicacao> findByPublicacaoIdAndUsuarioId(Long publicacaoId, Long usuarioId);
    long countByPublicacaoId(Long publicacaoId);
    void deleteByPublicacaoIdAndUsuarioId(Long publicacaoId, Long usuarioId);

    @Query("select c.publicacao.id, count(c.id) from CurtidaPublicacao c " +
            "where c.publicacao.id in :publicacaoIds group by c.publicacao.id")
    List<Object[]> contarPorPublicacoes(@Param("publicacaoIds") Collection<Long> publicacaoIds);

    @Query("select c.publicacao.id from CurtidaPublicacao c where c.usuario.id = :usuarioId " +
            "and c.publicacao.id in :publicacaoIds")
    List<Long> listarIdsCurtidos(@Param("usuarioId") Long usuarioId,
                                 @Param("publicacaoIds") Collection<Long> publicacaoIds);
}
