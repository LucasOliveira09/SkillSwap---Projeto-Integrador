package com.skillswap.api.domain.comentario;

import com.skillswap.api.domain.anuncio.Anuncio;
import com.skillswap.api.domain.anuncio.AnuncioRepository;
import com.skillswap.api.domain.anuncio.StatusAnuncio;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.infra.exception.ConflitoException;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComentarioAnuncioService {
    private final ComentarioAnuncioRepository comentarios;
    private final AnuncioRepository anuncios;
    private final UsuarioService usuarios;

    @Transactional(readOnly = true)
    public Page<ComentarioAnuncioDTO> listar(Long anuncioId, String email, Pageable pageable) {
        Anuncio anuncio = anuncioPublico(anuncioId);
        Long usuarioId = email == null ? null : usuarios.findByEmail(email).getId();
        return comentarios.listarVisiveis(anuncioId, pageable)
                .map(comentario -> toDto(comentario, usuarioId, anuncio.getUsuario().getId()));
    }

    @Transactional
    public ComentarioAnuncioDTO criar(Long anuncioId, ComentarioAnuncioRequestDTO dto, String email) {
        if (email == null) throw new NaoAutorizadoException("Entre na sua conta para comentar");
        Anuncio anuncio = anuncioPublico(anuncioId);
        Usuario autor = usuarios.findByEmail(email);
        ComentarioAnuncio respostaA = null;
        if (dto.respostaAId() != null) {
            respostaA = comentarios.findByIdAndAnuncioIdAndModeradaOcultaFalse(dto.respostaAId(), anuncioId)
                    .orElseThrow(() -> new NaoEncontradoException("Comentário não encontrado"));
            if (respostaA.getRespostaA() != null)
                throw new IllegalArgumentException("Respostas devem ser feitas ao comentário original");
            boolean autorDoComentario = respostaA.getAutor().getId().equals(autor.getId());
            boolean donoDoAnuncio = anuncio.getUsuario().getId().equals(autor.getId());
            if (!autorDoComentario && !donoDoAnuncio)
                throw new NaoAutorizadoException("Somente quem comentou ou o anunciante pode responder");
        }
        var comentario = comentarios.saveAndFlush(ComentarioAnuncio.builder()
                .anuncio(anuncio).autor(autor).respostaA(respostaA).conteudo(dto.conteudo().strip()).build());
        return toDto(comentario, autor.getId(), anuncio.getUsuario().getId());
    }

    @Transactional(readOnly = true)
    public ComentarioAnuncioDTO buscar(Long anuncioId, Long comentarioId, String email) {
        Anuncio anuncio = anuncioPublico(anuncioId);
        Long usuarioId = email == null ? null : usuarios.findByEmail(email).getId();
        return comentarios.findByIdAndAnuncioIdAndModeradaOcultaFalse(comentarioId, anuncioId)
                .map(comentario -> toDto(comentario, usuarioId, anuncio.getUsuario().getId()))
                .orElseThrow(() -> new NaoEncontradoException("Comentário não encontrado"));
    }

    @Transactional
    public ComentarioAnuncioDTO editar(Long anuncioId, Long comentarioId, EditarComentarioDTO dto, String email) {
        Anuncio anuncio = anuncioPublico(anuncioId);
        ComentarioAnuncio comentario = comentarioDoAutor(anuncioId, comentarioId, email);
        comentario.setConteudo(dto.conteudo().strip());
        return toDto(comentarios.saveAndFlush(comentario), comentario.getAutor().getId(), anuncio.getUsuario().getId());
    }

    /** Regra: comentário com respostas não pode ser excluído, para não deixar respostas órfãs (409). */
    @Transactional
    public void excluir(Long anuncioId, Long comentarioId, String email) {
        anuncioPublico(anuncioId);
        ComentarioAnuncio comentario = comentarioDoAutor(anuncioId, comentarioId, email);
        if (comentarios.existsByRespostaAId(comentario.getId()))
            throw new ConflitoException("Este comentário já tem respostas e não pode ser excluído; edite o texto.");
        comentarios.delete(comentario);
    }

    private ComentarioAnuncio comentarioDoAutor(Long anuncioId, Long comentarioId, String email) {
        ComentarioAnuncio comentario = comentarios.findByIdAndAnuncioIdAndModeradaOcultaFalse(comentarioId, anuncioId)
                .orElseThrow(() -> new NaoEncontradoException("Comentário não encontrado"));
        if (!comentario.getAutor().getEmail().equalsIgnoreCase(email))
            throw new NaoAutorizadoException("Somente o autor pode alterar o comentário");
        return comentario;
    }

    private Anuncio anuncioPublico(Long id) {
        return anuncios.findById(id)
                .filter(a -> a.getStatus() == StatusAnuncio.ATIVO && a.getUsuario().isPerfilPublico() && !a.isExemplo())
                .orElseThrow(() -> new NaoEncontradoException("Anúncio não encontrado"));
    }

    private ComentarioAnuncioDTO toDto(ComentarioAnuncio comentario, Long usuarioId, Long donoAnuncioId) {
        var autor = comentario.getAutor();
        String nome = autor.isPerfilPublico() ? autor.getNome() : "Usuário SkillSwap";
        boolean podeResponder = usuarioId != null && comentario.getRespostaA() == null
                && (autor.getId().equals(usuarioId) || donoAnuncioId.equals(usuarioId));
        return new ComentarioAnuncioDTO(comentario.getId(), comentario.getAnuncio().getId(),
                comentario.getRespostaA() == null || comentario.getRespostaA().isModeradaOculta()
                        ? null : comentario.getRespostaA().getId(),
                nome, comentario.getConteudo(), comentario.getCriadoEm(), podeResponder,
                usuarioId != null && !autor.getId().equals(usuarioId));
    }
}
