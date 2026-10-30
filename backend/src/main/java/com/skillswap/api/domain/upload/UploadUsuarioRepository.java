package com.skillswap.api.domain.upload;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface UploadUsuarioRepository extends JpaRepository<UploadUsuario, Long> {
    Optional<UploadUsuario> findByUsuarioIdAndNomeArquivo(Long usuarioId, String nomeArquivo);
    List<UploadUsuario> findByUsuarioIdAndTamanhoBytes(Long usuarioId, long tamanhoBytes);

    @Query("select coalesce(sum(upload.tamanhoBytes), 0) from UploadUsuario upload where upload.usuario.id = :usuarioId")
    long somarTamanhoPorUsuario(@Param("usuarioId") Long usuarioId);

    @Query("select case when count(upload) > 0 then true else false end from UploadUsuario upload " +
            "where upload.usuario.email = :email and upload.nomeArquivo = :nomeArquivo")
    boolean pertenceAoUsuario(@Param("email") String email, @Param("nomeArquivo") String nomeArquivo);

    @Transactional
    void deleteByNomeArquivo(String nomeArquivo);
}
