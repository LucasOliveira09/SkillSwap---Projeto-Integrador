package com.skillswap.api.domain.painel;

import com.skillswap.api.domain.acordo.Acordo;
import com.skillswap.api.domain.acordo.StatusAcordo;
import com.skillswap.api.domain.anuncio.StatusAnuncio;
import com.skillswap.api.domain.proposta.StatusProposta;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Consultas JPQL agregadas que alimentam os indicadores do dashboard. */
public interface PainelRepository extends Repository<Acordo, Long> {

    interface ContagemPorChave {
        Object getChave();
        Long getTotal();
    }

    // ---- Indicadores da conta autenticada ----

    @Query("select count(a) from Anuncio a where a.usuario.email = :email and a.status = :status")
    long contarAnunciosDoUsuario(@Param("email") String email, @Param("status") StatusAnuncio status);

    @Query("select count(p) from Proposta p where p.anuncio.usuario.email = :email and p.status = :status")
    long contarPropostasRecebidas(@Param("email") String email, @Param("status") StatusProposta status);

    @Query("""
            select count(p) from Proposta p where p.anuncio.usuario.email = :email
            and p.status <> com.skillswap.api.domain.proposta.StatusProposta.PENDENTE
            """)
    long contarPropostasRecebidasRespondidas(@Param("email") String email);

    @Query("select count(p) from Proposta p where p.proponente.email = :email and p.status = :status")
    long contarPropostasEnviadas(@Param("email") String email, @Param("status") StatusProposta status);

    @Query("""
            select count(a) from Acordo a
            where a.status = :status
              and (a.proposta.anuncio.usuario.email = :email or a.proposta.proponente.email = :email)
            """)
    long contarAcordosDoUsuario(@Param("email") String email, @Param("status") StatusAcordo status);

    @Query("select avg(av.nota) from Avaliacao av where av.avaliado.email = :email and av.moderadaOculta = false")
    Double mediaNotasRecebidas(@Param("email") String email);

    @Query("select count(av) from Avaliacao av where av.avaliado.email = :email and av.moderadaOculta = false")
    long contarAvaliacoesRecebidas(@Param("email") String email);

    // ---- Indicadores da plataforma (ADMIN) ----

    @Query("select count(u) from Usuario u")
    long contarUsuarios();

    @Query("select count(a) from Anuncio a where a.status = :status and a.exemplo = false")
    long contarAnuncios(@Param("status") StatusAnuncio status);

    @Query("select p.status as chave, count(p) as total from Proposta p group by p.status")
    List<ContagemPorChave> propostasPorStatus();

    @Query("select a.status as chave, count(a) as total from Acordo a group by a.status")
    List<ContagemPorChave> acordosPorStatus();

    @Query("""
            select a.categoria as chave, count(a) as total from Anuncio a
            where a.status = com.skillswap.api.domain.anuncio.StatusAnuncio.ATIVO and a.exemplo = false
            group by a.categoria order by count(a) desc
            """)
    List<ContagemPorChave> anunciosAtivosPorCategoria();

    @Query("select avg(av.nota) from Avaliacao av where av.moderadaOculta = false")
    Double mediaGeralAvaliacoes();
}
