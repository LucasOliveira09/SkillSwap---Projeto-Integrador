package com.skillswap.api.domain.acordo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AcordoRepository extends JpaRepository<Acordo, Long> {

    @EntityGraph(attributePaths = {"proposta", "proposta.anuncio", "proposta.anuncio.usuario", "proposta.proponente"})
    List<Acordo> findByPropostaAnuncioUsuarioIdOrPropostaProponenteId(Long anuncioUsuarioId, Long proponenteId);

    @EntityGraph(attributePaths = {"proposta", "proposta.anuncio", "proposta.anuncio.usuario", "proposta.proponente"})
    Optional<Acordo> findById(Long id);
}
