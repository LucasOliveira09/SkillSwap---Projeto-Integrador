package com.skillswap.api.domain.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
            select u from Usuario u
            where u.especialidades is not null and trim(u.especialidades) <> ''
              and (:busca = '' or lower(u.nome) like lower(concat('%', :busca, '%'))
                   or lower(u.especialidades) like lower(concat('%', :busca, '%')))
              and (:cidade = '' or lower(coalesce(u.cidade, '')) like lower(concat('%', :cidade, '%')))
            """)
    Page<Usuario> buscarProfissionais(
            @Param("busca") String busca,
            @Param("cidade") String cidade,
            Pageable pageable);
}
