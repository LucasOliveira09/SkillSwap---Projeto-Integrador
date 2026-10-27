package com.skillswap.api.domain.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.email = :email")
    Optional<Usuario> findByEmailForUpdate(@Param("email") String email);

    @Query("select u.fotoUrl from Usuario u where u.fotoUrl is not null")
    List<String> listarFotosDePerfil();

    @Query(value = """
            select u from Usuario u
            where u.especialidades is not null and trim(u.especialidades) <> ''
              and (:busca = '' or lower(u.nome) like lower(concat('%', :busca, '%'))
                   or lower(u.especialidades) like lower(concat('%', :busca, '%')))
              and (:cidade = '' or lower(coalesce(u.cidade, '')) like lower(concat('%', :cidade, '%')))
              and (:notaMinima is null or (
                   (select count(av.id) from Avaliacao av where av.avaliado = u and av.moderadaOculta = false) >= :amostraMinima
                   and (select avg(av.nota) from Avaliacao av where av.avaliado = u and av.moderadaOculta = false) >= :notaMinima))
            """, countQuery = """
            select count(u) from Usuario u
            where u.especialidades is not null and trim(u.especialidades) <> ''
              and (:busca = '' or lower(u.nome) like lower(concat('%', :busca, '%'))
                   or lower(u.especialidades) like lower(concat('%', :busca, '%')))
              and (:cidade = '' or lower(coalesce(u.cidade, '')) like lower(concat('%', :cidade, '%')))
              and (:notaMinima is null or (
                   (select count(av.id) from Avaliacao av where av.avaliado = u and av.moderadaOculta = false) >= :amostraMinima
                   and (select avg(av.nota) from Avaliacao av where av.avaliado = u and av.moderadaOculta = false) >= :notaMinima))
            """)
    Page<Usuario> buscarProfissionais(
            @Param("busca") String busca,
            @Param("cidade") String cidade,
            @Param("notaMinima") Double notaMinima,
            @Param("amostraMinima") int amostraMinima,
            Pageable pageable);
}
