package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.anuncio.CategoriaAnuncio;
import com.skillswap.api.domain.anuncio.ModalidadeAnuncio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByFotoUrlAndPerfilPublicoTrue(String fotoUrl);
    boolean existsByCapaUrlAndPerfilPublicoTrue(String capaUrl);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.email = :email")
    Optional<Usuario> findByEmailForUpdate(@Param("email") String email);

    @Query("select u.fotoUrl from Usuario u where u.fotoUrl is not null")
    List<String> listarFotosDePerfil();

    @Query("select u.capaUrl from Usuario u where u.capaUrl is not null")
    List<String> listarCapasDePerfil();

    Optional<Usuario> findByIdAndPerfilPublicoTrue(Long id);

    @Query(value = """
            select u from Usuario u
            where u.perfilPublico = true
              and ((u.especialidades is not null and trim(u.especialidades) <> '')
               or (u.tituloProfissional is not null and trim(u.tituloProfissional) <> ''))
              and (:busca = '' or lower(u.nome) like lower(concat('%', :busca, '%'))
                   or lower(coalesce(u.tituloProfissional, '')) like lower(concat('%', :busca, '%'))
                   or lower(u.especialidades) like lower(concat('%', :busca, '%')))
              and (:cidade = '' or lower(coalesce(u.cidade, '')) like lower(concat('%', :cidade, '%')))
              and (:notaMinima is null or (
                   (select count(av.id) from Avaliacao av where av.avaliado = u and av.moderadaOculta = false) >= :amostraMinima
                   and (select avg(av.nota) from Avaliacao av where av.avaliado = u and av.moderadaOculta = false) >= :notaMinima))
              and ((:categoria is null and :modalidade is null and :precoMinimo is null and :precoMaximo is null)
                   or exists (select a.id from Anuncio a where a.usuario = u and a.status = com.skillswap.api.domain.anuncio.StatusAnuncio.ATIVO
                       and a.exemplo = false and a.usuario.perfilPublico = true
                       and (:categoria is null or a.categoria = :categoria)
                       and (:modalidade is null or a.modalidade = :modalidade)
                       and (:precoMinimo is null or (a.preco is not null and a.preco >= :precoMinimo))
                       and (:precoMaximo is null or (a.preco is not null and a.preco <= :precoMaximo))))
            order by
              case
                when :busca <> '' and (
                    lower(coalesce(u.tituloProfissional, '')) = lower(:busca)
                    or lower(coalesce(u.especialidades, '')) = lower(:busca)) then 5
                when :busca <> '' and (
                    lower(coalesce(u.tituloProfissional, '')) like lower(concat(:busca, '%'))
                    or lower(coalesce(u.especialidades, '')) like lower(concat(:busca, '%'))) then 4
                when :busca <> '' and (
                    lower(coalesce(u.tituloProfissional, '')) like lower(concat('%', :busca, '%'))
                    or lower(coalesce(u.especialidades, '')) like lower(concat('%', :busca, '%'))) then 3
                when :busca <> '' and lower(u.nome) = lower(:busca) then 2
                when :busca <> '' and lower(u.nome) like lower(concat(:busca, '%')) then 1
                else 0
              end desc,
              case when :cidade <> '' and lower(coalesce(u.cidade, '')) = lower(:cidade) then 1 else 0 end desc,
              u.criadoEm desc, u.id desc
            """, countQuery = """
            select count(u) from Usuario u
            where u.perfilPublico = true
              and ((u.especialidades is not null and trim(u.especialidades) <> '')
               or (u.tituloProfissional is not null and trim(u.tituloProfissional) <> ''))
              and (:busca = '' or lower(u.nome) like lower(concat('%', :busca, '%'))
                   or lower(coalesce(u.tituloProfissional, '')) like lower(concat('%', :busca, '%'))
                   or lower(u.especialidades) like lower(concat('%', :busca, '%')))
              and (:cidade = '' or lower(coalesce(u.cidade, '')) like lower(concat('%', :cidade, '%')))
              and (:notaMinima is null or (
                   (select count(av.id) from Avaliacao av where av.avaliado = u and av.moderadaOculta = false) >= :amostraMinima
                   and (select avg(av.nota) from Avaliacao av where av.avaliado = u and av.moderadaOculta = false) >= :notaMinima))
              and ((:categoria is null and :modalidade is null and :precoMinimo is null and :precoMaximo is null)
                   or exists (select a.id from Anuncio a where a.usuario = u and a.status = com.skillswap.api.domain.anuncio.StatusAnuncio.ATIVO
                       and a.exemplo = false and a.usuario.perfilPublico = true
                       and (:categoria is null or a.categoria = :categoria)
                       and (:modalidade is null or a.modalidade = :modalidade)
                       and (:precoMinimo is null or (a.preco is not null and a.preco >= :precoMinimo))
                       and (:precoMaximo is null or (a.preco is not null and a.preco <= :precoMaximo))))
            """)
    Page<Usuario> buscarProfissionais(
            @Param("busca") String busca,
            @Param("cidade") String cidade,
            @Param("notaMinima") Double notaMinima,
            @Param("amostraMinima") int amostraMinima,
            @Param("categoria") CategoriaAnuncio categoria,
            @Param("modalidade") ModalidadeAnuncio modalidade,
            @Param("precoMinimo") BigDecimal precoMinimo,
            @Param("precoMaximo") BigDecimal precoMaximo,
            Pageable pageable);
}
