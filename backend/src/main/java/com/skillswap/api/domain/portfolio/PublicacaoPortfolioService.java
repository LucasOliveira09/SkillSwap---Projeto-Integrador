package com.skillswap.api.domain.portfolio;

import com.skillswap.api.domain.anuncio.AnuncioRepository;
import com.skillswap.api.domain.anuncio.StatusAnuncio;
import com.skillswap.api.domain.portfolio.dto.CriarPublicacaoPortfolioDTO;
import com.skillswap.api.domain.portfolio.dto.AtualizarLegendaPublicacaoDTO;
import com.skillswap.api.domain.portfolio.dto.PublicacaoPortfolioDTO;
import com.skillswap.api.domain.portfolio.dto.CurtidaPublicacaoDTO;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicacaoPortfolioService {
    private static final java.util.regex.Pattern FOTO_LOCAL = java.util.regex.Pattern
            .compile("^/api/uploads/[a-f0-9]{64}\\.(jpg|png|gif)$");

    private final PublicacaoPortfolioRepository publicacaoRepository;
    private final UsuarioService usuarioService;
    private final AnuncioRepository anuncioRepository;
    private final UploadService uploadService;
    private final CurtidaPublicacaoRepository curtidas;

    @Transactional(readOnly = true)
    public Page<PublicacaoPortfolioDTO> listar(Long usuarioId, Pageable pageable, String emailVisualizador) {
        usuarioService.buscarPerfilPublico(usuarioId);
        Long visualizadorId = emailVisualizador == null ? null : usuarioService.findByEmail(emailVisualizador).getId();
        return paraDTO(publicacaoRepository.findByUsuarioIdAndModeradaOcultaFalse(usuarioId, pageable), visualizadorId);
    }

    @Transactional(readOnly = true)
    public Page<PublicacaoPortfolioDTO> listarProprias(String email, Pageable pageable) {
        var usuario = usuarioService.findByEmail(email);
        return paraDTO(publicacaoRepository.findByUsuarioIdAndModeradaOcultaFalse(usuario.getId(), pageable), usuario.getId());
    }

    @Transactional(readOnly = true)
    public PublicacaoPortfolioDTO buscar(Long id) {
        var publicacao = publicacaoRepository.findByIdAndModeradaOcultaFalse(id)
                .orElseThrow(() -> new NaoEncontradoException("Publicação não encontrada"));
        usuarioService.buscarPerfilPublico(publicacao.getUsuario().getId());
        return paraDTO(publicacao, 0, false);
    }

    @Transactional
    public CurtidaPublicacaoDTO alterarCurtida(Long id, String email, boolean curtir) {
        Usuario usuario = usuarioService.findByEmail(email);
        PublicacaoPortfolio publicacao = publicacaoRepository.findVisivelPorIdParaAtualizar(id)
                .filter(p -> p.getUsuario().isPerfilPublico())
                .orElseThrow(() -> new NaoEncontradoException("Publicação não encontrada"));
        boolean existe = curtidas.existsByPublicacaoIdAndUsuarioId(id, usuario.getId());
        if (curtir && !existe) {
            curtidas.saveAndFlush(CurtidaPublicacao.builder().publicacao(publicacao).usuario(usuario).build());
        } else if (!curtir && existe) {
            curtidas.deleteByPublicacaoIdAndUsuarioId(id, usuario.getId());
        }
        return new CurtidaPublicacaoDTO(id, curtidas.countByPublicacaoId(id), curtir);
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
        return paraDTO(p, curtidas.countByPublicacaoId(p.getId()), false);
    }

    private PublicacaoPortfolioDTO paraDTO(PublicacaoPortfolio p, long totalCurtidas, boolean curtidaPeloUsuario) {
        return new PublicacaoPortfolioDTO(
                p.getId(), p.getUsuario().getId(), p.getUsuario().getNome(),
                p.getAnuncio() == null ? null : p.getAnuncio().getId(),
                p.getAnuncio() == null ? null : p.getAnuncio().getTitulo(), p.getLegenda(),
                List.copyOf(p.getFotos()), p.getCriadoEm(), totalCurtidas, curtidaPeloUsuario);
    }

    private Page<PublicacaoPortfolioDTO> paraDTO(Page<PublicacaoPortfolio> pagina, Long visualizadorId) {
        var ids = pagina.getContent().stream().map(PublicacaoPortfolio::getId).toList();
        if (ids.isEmpty()) return pagina.map(p -> paraDTO(p, 0, false));
        Map<Long, Long> totais = curtidas.contarPorPublicacoes(ids).stream().collect(Collectors.toMap(
                linha -> (Long) linha[0], linha -> ((Number) linha[1]).longValue()));
        Set<Long> curtidasDoVisualizador = visualizadorId == null ? Set.of()
                : Set.copyOf(curtidas.listarIdsCurtidos(visualizadorId, ids));
        return pagina.map(publicacao -> paraDTO(publicacao, totais.getOrDefault(publicacao.getId(), 0L),
                curtidasDoVisualizador.contains(publicacao.getId())));
    }
}
