package com.skillswap.api.domain.moderacao;

import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import com.skillswap.api.domain.avaliacao.AvaliacaoRepository;
import com.skillswap.api.domain.moderacao.dto.CriarDenunciaDTO;
import com.skillswap.api.domain.moderacao.dto.DecisaoModeracaoDTO;
import com.skillswap.api.domain.moderacao.dto.DenunciaDTO;
import com.skillswap.api.domain.portfolio.PublicacaoPortfolioRepository;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.infra.exception.ConflitoException;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class ModeracaoService {
    private final DenunciaRepository denunciaRepository;
    private final PublicacaoPortfolioRepository publicacaoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioService usuarioService;

    @Value("${skillswap.moderacao.emails:}")
    private String emailsModeradores;

    public boolean podeModerar(String email) {
        return Arrays.stream(emailsModeradores.split(","))
                .map(String::strip)
                .anyMatch(configurado -> !configurado.isEmpty() && configurado.equalsIgnoreCase(email));
    }

    @Transactional
    public DenunciaDTO denunciar(CriarDenunciaDTO dto, String email) {
        var autor = usuarioService.findByEmail(email);
        String trecho = validarEObterTrecho(dto.tipoConteudo(), dto.recursoId(), autor.getId());
        if (denunciaRepository.existsByAutorIdAndTipoConteudoAndRecursoId(
                autor.getId(), dto.tipoConteudo(), dto.recursoId())) {
            throw new ConflitoException("Você já denunciou este conteúdo.");
        }
        var denuncia = Denuncia.builder()
                .autor(autor)
                .tipoConteudo(dto.tipoConteudo())
                .recursoId(dto.recursoId())
                .motivo(dto.motivo())
                .descricao(dto.descricao() == null ? null : dto.descricao().strip())
                .trechoConteudo(trecho)
                .build();
        try {
            return paraDTO(denunciaRepository.saveAndFlush(denuncia));
        } catch (DataIntegrityViolationException duplicada) {
            if (denunciaRepository.existsByAutorIdAndTipoConteudoAndRecursoId(
                    autor.getId(), dto.tipoConteudo(), dto.recursoId())) {
                throw new ConflitoException("Você já denunciou este conteúdo.");
            }
            throw duplicada;
        }
    }

    @Transactional(readOnly = true)
    public DenunciaDTO buscar(Long id, String email) {
        var denuncia = denunciaRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Denúncia não encontrada"));
        if (!podeModerar(email) && !denuncia.getAutor().getEmail().equalsIgnoreCase(email)) {
            throw new NaoAutorizadoException("Você não pode consultar esta denúncia.");
        }
        return paraDTO(denuncia);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<DenunciaDTO> listar(StatusDenuncia status, int pagina, int tamanho, String email) {
        exigirModerador(email);
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.ASC, "criadaEm", "id"));
        return PaginaDTO.de(denunciaRepository.findByStatusOrderByCriadaEmAscIdAsc(status, pageable)
                .map(this::paraDTO));
    }

    @Transactional
    public DenunciaDTO decidir(Long id, DecisaoModeracaoDTO dto, String email) {
        exigirModerador(email);
        var denuncia = denunciaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NaoEncontradoException("Denúncia não encontrada"));
        switch (dto.acao()) {
            case OCULTAR -> {
                exigirStatus(denuncia, StatusDenuncia.ABERTA);
                ocultar(denuncia);
                denuncia.setStatus(StatusDenuncia.OCULTADA);
            }
            case DESCARTAR -> {
                exigirStatus(denuncia, StatusDenuncia.ABERTA);
                denuncia.setStatus(StatusDenuncia.DESCARTADA);
            }
            case REEXIBIR -> {
                exigirStatus(denuncia, StatusDenuncia.OCULTADA);
                reexibir(denuncia);
                denuncia.setStatus(StatusDenuncia.REEXIBIDA);
            }
        }
        denuncia.setAnalisadaEm(LocalDateTime.now());
        denuncia.setAnalisadaPor(email);
        return paraDTO(denunciaRepository.save(denuncia));
    }

    private String validarEObterTrecho(TipoConteudoDenunciado tipo, Long recursoId, Long autorId) {
        return switch (tipo) {
            case PUBLICACAO -> publicacaoRepository.findByIdAndModeradaOcultaFalse(recursoId)
                    .map(publicacao -> {
                        if (publicacao.getUsuario().getId().equals(autorId)) {
                            throw new IllegalArgumentException("Não é possível denunciar seu próprio conteúdo.");
                        }
                        return publicacao.getLegenda();
                    }).orElseThrow(() -> new NaoEncontradoException("Publicação não encontrada"));
            case AVALIACAO -> avaliacaoRepository.findById(recursoId)
                    .filter(avaliacao -> !avaliacao.isModeradaOculta())
                    .map(avaliacao -> {
                        if (avaliacao.getAvaliador().getId().equals(autorId)) {
                            throw new IllegalArgumentException("Não é possível denunciar sua própria avaliação.");
                        }
                        return avaliacao.getDepoimento();
                    }).orElseThrow(() -> new NaoEncontradoException("Avaliação não encontrada"));
        };
    }

    private void ocultar(Denuncia denuncia) {
        switch (denuncia.getTipoConteudo()) {
            case PUBLICACAO -> publicacaoRepository.findById(denuncia.getRecursoId())
                    .ifPresent(publicacao -> publicacao.setModeradaOculta(true));
            case AVALIACAO -> avaliacaoRepository.findById(denuncia.getRecursoId())
                    .ifPresent(avaliacao -> avaliacao.setModeradaOculta(true));
        }
    }

    private void reexibir(Denuncia denuncia) {
        switch (denuncia.getTipoConteudo()) {
            case PUBLICACAO -> publicacaoRepository.findById(denuncia.getRecursoId())
                    .ifPresent(publicacao -> publicacao.setModeradaOculta(false));
            case AVALIACAO -> avaliacaoRepository.findById(denuncia.getRecursoId())
                    .ifPresent(avaliacao -> avaliacao.setModeradaOculta(false));
        }
    }

    private void exigirStatus(Denuncia denuncia, StatusDenuncia esperado) {
        if (denuncia.getStatus() != esperado) throw new ConflitoException("A denúncia não está no status exigido para esta ação.");
    }

    private void exigirModerador(String email) {
        if (!podeModerar(email)) throw new NaoAutorizadoException("Acesso restrito à equipe de moderação.");
    }

    private DenunciaDTO paraDTO(Denuncia denuncia) {
        return new DenunciaDTO(denuncia.getId(), denuncia.getTipoConteudo(), denuncia.getRecursoId(),
                denuncia.getMotivo(), denuncia.getDescricao(), denuncia.getStatus(), denuncia.getCriadaEm(),
                denuncia.getAnalisadaEm(), denuncia.getAnalisadaPor(), denuncia.getAutor().getId(),
                denuncia.getAutor().getNome(), denuncia.getTrechoConteudo());
    }
}
