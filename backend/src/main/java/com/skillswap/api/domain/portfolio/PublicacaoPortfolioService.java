package com.skillswap.api.domain.portfolio;

import com.skillswap.api.domain.anuncio.AnuncioRepository;
import com.skillswap.api.domain.anuncio.StatusAnuncio;
import com.skillswap.api.domain.portfolio.dto.CriarPublicacaoPortfolioDTO;
import com.skillswap.api.domain.portfolio.dto.AtualizarLegendaPublicacaoDTO;
import com.skillswap.api.domain.portfolio.dto.PublicacaoPortfolioDTO;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.domain.upload.UploadService;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicacaoPortfolioService {
    private static final java.util.regex.Pattern FOTO_LOCAL = java.util.regex.Pattern
            .compile("^/api/uploads/[a-f0-9]{64}\\.(jpg|png|gif)$");

    private final PublicacaoPortfolioRepository publicacaoRepository;
    private final UsuarioService usuarioService;
    private final AnuncioRepository anuncioRepository;
    private final UploadService uploadService;

    @Transactional(readOnly = true)
    public Page<PublicacaoPortfolioDTO> listar(Long usuarioId, Pageable pageable) {
        return publicacaoRepository.findByUsuarioId(usuarioId, pageable).map(this::paraDTO);
    }

    @Transactional(readOnly = true)
    public PublicacaoPortfolioDTO buscar(Long id) {
        return publicacaoRepository.findById(id)
                .map(this::paraDTO)
                .orElseThrow(() -> new NaoEncontradoException("Publicação não encontrada"));
    }

    @Transactional
    public PublicacaoPortfolioDTO criar(CriarPublicacaoPortfolioDTO dto, String email) {
        Usuario autor = usuarioService.findByEmail(email);
        List<String> fotos = dto.fotos().stream().map(String::strip).distinct().toList();
        if (fotos.size() != dto.fotos().size() || fotos.stream().anyMatch(foto -> !FOTO_LOCAL.matcher(foto).matches())) {
            throw new IllegalArgumentException("Use de 1 a 6 fotos enviadas para o SkillSwap, sem duplicatas");
        }
        fotos.forEach(uploadService::validarFotoPublica);
        var publicacao = PublicacaoPortfolio.builder()
                .usuario(autor)
                .legenda(dto.legenda() == null ? "" : dto.legenda().strip())
                .fotos(fotos)
                .build();
        if (dto.anuncioId() != null) {
            var anuncio = anuncioRepository.findById(dto.anuncioId())
                    .orElseThrow(() -> new NaoEncontradoException("Oferta não encontrada"));
            if (anuncio.getStatus() != StatusAnuncio.ATIVO || anuncio.isExemplo()) {
                throw new NaoEncontradoException("Oferta não encontrada");
            }
            if (!anuncio.getUsuario().getId().equals(autor.getId())) {
                throw new NaoAutorizadoException("A publicação só pode ser vinculada à sua própria oferta");
            }
            publicacao.setAnuncio(anuncio);
        }
        return paraDTO(publicacaoRepository.saveAndFlush(publicacao));
    }

    @Transactional
    public void excluir(Long id, String email) {
        var publicacao = publicacaoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Publicação não encontrada"));
        if (!publicacao.getUsuario().getEmail().equals(email)) {
            throw new NaoAutorizadoException("Você não pode remover esta publicação");
        }
        publicacaoRepository.delete(publicacao);
    }

    @Transactional
    public PublicacaoPortfolioDTO atualizarLegenda(Long id, AtualizarLegendaPublicacaoDTO dto, String email) {
        var publicacao = publicacaoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Publicação não encontrada"));
        validarAutor(publicacao, email);
        publicacao.setLegenda(dto.legenda().strip());
        return paraDTO(publicacaoRepository.saveAndFlush(publicacao));
    }

    private void validarAutor(PublicacaoPortfolio publicacao, String email) {
        if (!publicacao.getUsuario().getEmail().equals(email)) {
            throw new NaoAutorizadoException("Você não pode alterar esta publicação");
        }
    }

    private PublicacaoPortfolioDTO paraDTO(PublicacaoPortfolio p) {
        return new PublicacaoPortfolioDTO(
                p.getId(), p.getUsuario().getId(), p.getUsuario().getNome(),
                p.getAnuncio() == null ? null : p.getAnuncio().getId(),
                p.getAnuncio() == null ? null : p.getAnuncio().getTitulo(),
                p.getLegenda(), List.copyOf(p.getFotos()), p.getCriadoEm());
    }
}
