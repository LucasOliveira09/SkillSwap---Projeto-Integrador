package com.skillswap.api.domain.comentario;

import com.skillswap.api.domain.portfolio.PublicacaoPortfolio;
import com.skillswap.api.domain.portfolio.PublicacaoPortfolioRepository;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComentarioPublicacaoService {
    private final ComentarioPublicacaoRepository comentarios;
    private final PublicacaoPortfolioRepository publicacoes;
    private final UsuarioService usuarios;

    @Transactional(readOnly = true)
    public Page<ComentarioPublicacaoDTO> listar(Long publicacaoId, String email, Pageable pageable) {
        PublicacaoPortfolio publicacao = publicacaoPublica(publicacaoId);
        Long usuarioId = email == null ? null : usuarios.findByEmail(email).getId();
        Page<ComentarioPublicacao> raizes = comentarios
                .findByPublicacaoIdAndRespostaAIsNullAndModeradaOcultaFalse(publicacaoId, pageable);
        var idsRaizes = raizes.getContent().stream().map(ComentarioPublicacao::getId).toList();
        Map<Long, java.util.List<ComentarioPublicacao>> respostas = idsRaizes.isEmpty() ? Map.of()
                : comentarios.findByRespostaAIdInAndModeradaOcultaFalseOrderByCriadoEmAscIdAsc(idsRaizes).stream()
                    .collect(Collectors.groupingBy(c -> c.getRespostaA().getId()));
        return raizes.map(raiz -> paraDTO(raiz, usuarioId, publicacao.getUsuario().getId(),
                respostas.getOrDefault(raiz.getId(), java.util.List.of())));
    }

    @Transactional(readOnly = true)
    public ComentarioPublicacaoDTO buscar(Long publicacaoId, Long comentarioId, String email) {
        PublicacaoPortfolio publicacao = publicacaoPublica(publicacaoId);
        Long usuarioId = email == null ? null : usuarios.findByEmail(email).getId();
        ComentarioPublicacao comentario = comentarios.findByIdAndPublicacaoIdAndModeradaOcultaFalse(comentarioId, publicacaoId)
                .orElseThrow(() -> new NaoEncontradoException("Comentário não encontrado"));
        var respostas = comentarios.findByRespostaAIdInAndModeradaOcultaFalseOrderByCriadoEmAscIdAsc(
                java.util.List.of(comentario.getId()));
        return paraDTO(comentario, usuarioId, publicacao.getUsuario().getId(), respostas);
    }

    @Transactional
    public ComentarioPublicacaoDTO criar(Long publicacaoId, ComentarioPublicacaoRequestDTO dto, String email) {
        if (email == null) throw new NaoAutorizadoException("Entre na sua conta para comentar");
        PublicacaoPortfolio publicacao = publicacaoPublica(publicacaoId);
        Usuario autor = usuarios.findByEmail(email);
        ComentarioPublicacao respostaA = null;
        if (dto.respostaAId() != null) {
            respostaA = comentarios.findByIdAndPublicacaoIdAndModeradaOcultaFalse(dto.respostaAId(), publicacaoId)
                    .orElseThrow(() -> new NaoEncontradoException("Comentário não encontrado"));
            if (respostaA.getRespostaA() != null)
                throw new IllegalArgumentException("Respostas devem ser feitas ao comentário original");
            boolean autorDaRaiz = respostaA.getAutor().getId().equals(autor.getId());
            boolean donoDaPublicacao = publicacao.getUsuario().getId().equals(autor.getId());
            if (!autorDaRaiz && !donoDaPublicacao)
                throw new NaoAutorizadoException("Somente quem comentou ou o profissional pode responder");
        }
        ComentarioPublicacao salvo = comentarios.saveAndFlush(ComentarioPublicacao.builder()
                .publicacao(publicacao).autor(autor).respostaA(respostaA).conteudo(dto.conteudo().strip()).build());
        return paraDTO(salvo, autor.getId(), publicacao.getUsuario().getId(), java.util.List.of());
    }

    @Transactional
    public ComentarioPublicacaoDTO editar(Long publicacaoId, Long comentarioId, EditarComentarioDTO dto, String email) {
        PublicacaoPortfolio publicacao = publicacaoPublica(publicacaoId);
        ComentarioPublicacao comentario = comentarioDoAutor(publicacaoId, comentarioId, email);
        comentario.setConteudo(dto.conteudo().strip());
        return paraDTO(comentarios.saveAndFlush(comentario), comentario.getAutor().getId(),
                publicacao.getUsuario().getId(), java.util.List.of());
    }

    /** Regra: comentário com respostas não pode ser excluído (409). */
    @Transactional
    public void excluir(Long publicacaoId, Long comentarioId, String email) {
        publicacaoPublica(publicacaoId);
        ComentarioPublicacao comentario = comentarioDoAutor(publicacaoId, comentarioId, email);
        if (comentarios.existsByRespostaAId(comentario.getId()))
            throw new com.skillswap.api.infra.exception.ConflitoException(
                    "Este comentário já tem respostas e não pode ser excluído; edite o texto.");
        comentarios.delete(comentario);
    }

    private ComentarioPublicacao comentarioDoAutor(Long publicacaoId, Long comentarioId, String email) {
        ComentarioPublicacao comentario = comentarios.findByIdAndPublicacaoIdAndModeradaOcultaFalse(comentarioId, publicacaoId)
                .orElseThrow(() -> new NaoEncontradoException("Comentário não encontrado"));
        if (!comentario.getAutor().getEmail().equalsIgnoreCase(email))
            throw new NaoAutorizadoException("Somente o autor pode alterar o comentário");
        return comentario;
    }

    private PublicacaoPortfolio publicacaoPublica(Long id) {
        PublicacaoPortfolio publicacao = publicacoes.findByIdAndModeradaOcultaFalse(id)
                .filter(p -> p.getUsuario().isPerfilPublico())
                .orElseThrow(() -> new NaoEncontradoException("Publicação não encontrada"));
        usuarios.buscarPerfilPublico(publicacao.getUsuario().getId());
        return publicacao;
    }

    private ComentarioPublicacaoDTO paraDTO(ComentarioPublicacao comentario, Long usuarioId,
            Long profissionalId, java.util.List<ComentarioPublicacao> respostas) {
        Long autorId = comentario.getAutor().getId();
        boolean raiz = comentario.getRespostaA() == null;
        boolean podeResponder = usuarioId != null && raiz
                && (autorId.equals(usuarioId) || profissionalId.equals(usuarioId));
        var respostasDTO = respostas.stream().map(resposta -> paraDTO(resposta, usuarioId, profissionalId,
                java.util.List.of())).toList();
        return new ComentarioPublicacaoDTO(comentario.getId(), comentario.getPublicacao().getId(),
                comentario.getRespostaA() == null ? null : comentario.getRespostaA().getId(),
                comentario.getAutor().isPerfilPublico() ? comentario.getAutor().getNome() : "Usuário SkillSwap",
                comentario.getConteudo(), comentario.getCriadoEm(), respostasDTO, podeResponder,
                usuarioId != null && !autorId.equals(usuarioId));
    }
}
