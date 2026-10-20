package br.edu.unifio.skillswap.repositories;

import br.edu.unifio.skillswap.entities.Anuncio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnuncioRepository extends JpaRepository<Anuncio, Integer> {
    @EntityGraph(attributePaths = "categoria")
    @Query("select a from Anuncio a where (:categoria = '' or a.categoria.codigo = :categoria) "
         + "and a.textoBusca like :busca escape '!' order by a.criadoEm desc, a.id desc")
    Page<Anuncio> pesquisar(@Param("categoria") String categoria, @Param("busca") String busca, Pageable pagina);

    boolean existsByCodigoLegado(String codigoLegado);
}
