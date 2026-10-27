package com.skillswap.api.domain.acordo;

import com.skillswap.api.domain.acordo.dto.AcordoResponseDTO;
import com.skillswap.api.domain.acordo.dto.AtualizarStatusAcordoDTO;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.domain.notificacao.NotificacaoService;
import com.skillswap.api.domain.notificacao.TipoNotificacao;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AcordoService {

    private final AcordoRepository acordoRepository;
    private final UsuarioService usuarioService;
    private final NotificacaoService notificacaoService;

    @Transactional(readOnly = true)
    public List<AcordoResponseDTO> listarMeus(String emailUsuario) {
        Usuario usuario = usuarioService.findByEmail(emailUsuario);
        return acordoRepository.findByPropostaAnuncioUsuarioIdOrPropostaProponenteId(
                        usuario.getId(), usuario.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AcordoResponseDTO> listarMeus(String emailUsuario, Pageable pageable) {
        Usuario usuario = usuarioService.findByEmail(emailUsuario);
        return acordoRepository.findByPropostaAnuncioUsuarioIdOrPropostaProponenteId(
                        usuario.getId(), usuario.getId(), pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public AcordoResponseDTO buscar(Long id, String emailUsuario) {
        Acordo acordo = findById(id);
        checkParticipant(acordo, emailUsuario);
        return toResponseDTO(acordo);
    }

    @Transactional
    public Acordo atualizarStatus(Long id, AtualizarStatusAcordoDTO dto, String emailUsuario) {
        return switch (dto.status()) {
            case CONCLUIDO -> concluir(id, emailUsuario);
            case CANCELADO -> cancelar(id, emailUsuario);
            default -> throw new IllegalArgumentException("Status solicitado não é permitido nesta operação");
        };
    }

    @Transactional
    public Acordo concluir(Long id, String emailUsuario) {
        Acordo acordo = acordoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NaoEncontradoException("Acordo não encontrado"));
        checkParticipant(acordo, emailUsuario);
        if (acordo.getStatus() != StatusAcordo.EM_ANDAMENTO) {
            throw new IllegalArgumentException("Acordo não está em andamento");
        }
        Long anuncianteId = acordo.getProposta().getAnuncio().getUsuario().getId();
        Long usuarioId = usuarioService.findByEmail(emailUsuario).getId();
        if (anuncianteId.equals(usuarioId)) {
            acordo.setConclusaoConfirmadaAnunciante(true);
        } else {
            acordo.setConclusaoConfirmadaProponente(true);
        }
        if (Boolean.TRUE.equals(acordo.getConclusaoConfirmadaAnunciante())
                && Boolean.TRUE.equals(acordo.getConclusaoConfirmadaProponente())) {
            acordo.setStatus(StatusAcordo.CONCLUIDO);
            acordo.setConcluidoEm(LocalDateTime.now());
        }
        Usuario anunciante = acordo.getProposta().getAnuncio().getUsuario();
        Usuario destinatario = anunciante.getId().equals(usuarioId)
                ? acordo.getProposta().getProponente()
                : anunciante;
        boolean concluido = acordo.getStatus() == StatusAcordo.CONCLUIDO;
        notificacaoService.criar(destinatario, TipoNotificacao.ACORDO_ATUALIZADO,
                concluido ? "Acordo concluído" : "Confirmação de conclusão",
                concluido ? "As duas pessoas confirmaram a conclusão de “" + acordo.getProposta().getAnuncio().getTitulo() + "”. Avaliações liberadas."
                        : "A outra pessoa confirmou a conclusão de “" + acordo.getProposta().getAnuncio().getTitulo() + "”. Confirme para concluir.",
                "/acordos/" + acordo.getId());
        return acordoRepository.save(acordo);
    }

    @Transactional
    public Acordo cancelar(Long id, String emailUsuario) {
        Acordo acordo = acordoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NaoEncontradoException("Acordo não encontrado"));
        checkParticipant(acordo, emailUsuario);
        if (acordo.getStatus() != StatusAcordo.EM_ANDAMENTO) {
            throw new IllegalArgumentException("Acordo não está em andamento");
        }
        acordo.setStatus(StatusAcordo.CANCELADO);
        Usuario atual = usuarioService.findByEmail(emailUsuario);
        Usuario anunciante = acordo.getProposta().getAnuncio().getUsuario();
        Usuario destinatario = anunciante.getId().equals(atual.getId())
                ? acordo.getProposta().getProponente()
                : anunciante;
        notificacaoService.criar(destinatario, TipoNotificacao.ACORDO_ATUALIZADO,
                "Acordo cancelado", "O acordo para “" + acordo.getProposta().getAnuncio().getTitulo() + "” foi cancelado.",
                "/acordos/" + acordo.getId());
        return acordoRepository.save(acordo);
    }

    public Acordo findById(Long id) {
        return acordoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Acordo não encontrado"));
    }

    private void checkParticipant(Acordo acordo, String emailUsuario) {
        String emailAnunciante = acordo.getProposta().getAnuncio().getUsuario().getEmail();
        String emailProponente = acordo.getProposta().getProponente().getEmail();
        if (!emailAnunciante.equals(emailUsuario) && !emailProponente.equals(emailUsuario)) {
            throw new NaoAutorizadoException("Você não é participante deste acordo");
        }
    }

    public AcordoResponseDTO toResponseDTO(Acordo a) {
        var proposta = a.getProposta();
        var anuncio = proposta.getAnuncio();
        return new AcordoResponseDTO(
                a.getId(),
                proposta.getId(),
                anuncio.getId(),
                anuncio.getTitulo(),
                anuncio.getUsuario().getId(),
                anuncio.getUsuario().getNome(),
                proposta.getProponente().getId(),
                proposta.getProponente().getNome(),
                a.getStatus(),
                a.getCriadoEm(),
                a.getConcluidoEm(),
                a.getConclusaoConfirmadaAnunciante(),
                a.getConclusaoConfirmadaProponente()
        );
    }
}
