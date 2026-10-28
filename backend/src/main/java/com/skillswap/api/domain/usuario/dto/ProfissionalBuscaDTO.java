package com.skillswap.api.domain.usuario.dto;

import com.skillswap.api.domain.anuncio.dto.ResumoOfertaProfissionalDTO;
import java.time.LocalDateTime;
import java.util.List;

public record ProfissionalBuscaDTO(
        Long id,
        String nome,
        String tituloProfissional,
        String bio,
        String cidade,
        String fotoUrl,
        String capaUrl,
        String linkedinUrl,
        String instagramUrl,
        String siteUrl,
        boolean perfilPublico,
        String especialidades,
        LocalDateTime criadoEm,
        Double mediaNotas,
        long quantidadeAvaliacoes,
        List<ResumoOfertaProfissionalDTO> resumoOfertas
) {
    public static ProfissionalBuscaDTO de(PerfilDTO perfil, List<ResumoOfertaProfissionalDTO> ofertas) {
        return new ProfissionalBuscaDTO(
                perfil.id(), perfil.nome(), perfil.tituloProfissional(), perfil.bio(), perfil.cidade(),
                perfil.fotoUrl(), perfil.capaUrl(), perfil.linkedinUrl(), perfil.instagramUrl(), perfil.siteUrl(),
                perfil.perfilPublico(), perfil.especialidades(), perfil.criadoEm(), perfil.mediaNotas(),
                perfil.quantidadeAvaliacoes(), List.copyOf(ofertas));
    }
}
