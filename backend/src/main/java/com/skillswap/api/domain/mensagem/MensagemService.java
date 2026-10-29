package com.skillswap.api.domain.mensagem;

import com.skillswap.api.domain.acordo.Acordo;
import com.skillswap.api.domain.acordo.AcordoService;
import com.skillswap.api.domain.acordo.StatusAcordo;
import com.skillswap.api.domain.mensagem.dto.MensagemRequestDTO;
import com.skillswap.api.domain.mensagem.dto.MensagemResponseDTO;
import com.skillswap.api.domain.notificacao.NotificacaoService;
import com.skillswap.api.domain.notificacao.TipoNotificacao;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MensagemService {

    private final MensagemRepository mensagemRepository;
    private final AcordoService acordoService;
    private final UsuarioService usuarioService;
    private final NotificacaoService notificacaoService;

    @Transactional
    public Mensagem enviar(Long acordoId, MensagemRequestDTO dto, String emailUsuario) {
        Acordo acordo = acordoService.findById(acordoId);
        checkParticipant(acordo, emailUsuario);
        if (acordo.getStatus() != StatusAcordo.EM_ANDAMENTO) {
            throw new IllegalArgumentException("Não é possível enviar mensagens em um acordo encerrado");
        }
        Usuario remetente = usuarioService.findByEmail(emailUsuario);

        var mensagem = Mensagem.builder()
                .acordo(acordo)
                .proposta(acordo.getProposta())
                .remetente(remetente)
                .conteudo(dto.conteudo())
                .build();
        mensagem = mensagemRepository.save(mensagem);
        Usuario anunciante = acordo.getProposta().getAnuncio().getUsuario();
        Usuario destinatario = anunciante.getId().equals(remetente.getId())
                ? acordo.getProposta().getProponente()
                : anunciante;
        notificacaoService.criar(destinatario, TipoNotificacao.NOVA_MENSAGEM,
                "Nova mensagem", remetente.getNome() + " enviou uma mensagem sobre “" + acordo.getProposta().getAnuncio().getTitulo() + "”.",
                "/mensagens?conversa=" + acordo.getProposta().getId());
        return mensagem;
    }

    @Transactional(readOnly = true)
    public List<MensagemResponseDTO> listarPorAcordo(Long acordoId, String emailUsuario) {
        Acordo acordo = acordoService.findById(acordoId);
        checkParticipant(acordo, emailUsuario);
        return mensagemRepository.findByAcordoIdOrderByEnviadoEmAsc(acordoId).stream()
                .map(m -> toResponseDTO(m, acordoId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<MensagemResponseDTO> listarPorAcordo(Long acordoId, String emailUsuario, Pageable pageable) {
        Acordo acordo = acordoService.findById(acordoId);
        checkParticipant(acordo, emailUsuario);
        return mensagemRepository.findByAcordoId(acordoId, pageable).map(m -> toResponseDTO(m, acordoId));
    }

    @Transactional(readOnly = true)
    public MensagemResponseDTO buscar(Long acordoId, Long mensagemId, String emailUsuario) {
        Acordo acordo = acordoService.findById(acordoId);
        checkParticipant(acordo, emailUsuario);
        return mensagemRepository.findByIdAndAcordoId(mensagemId, acordoId)
                .map(m -> toResponseDTO(m, acordoId))
                .orElseThrow(() -> new com.skillswap.api.infra.exception.NaoEncontradoException("Mensagem não encontrada"));
    }

    private void checkParticipant(Acordo acordo, String emailUsuario) {
        String emailAnunciante = acordo.getProposta().getAnuncio().getUsuario().getEmail();
        String emailProponente = acordo.getProposta().getProponente().getEmail();
        if (!emailAnunciante.equals(emailUsuario) && !emailProponente.equals(emailUsuario)) {
            throw new NaoAutorizadoException("Você não é participante deste acordo");
        }
    }

    public MensagemResponseDTO toResponseDTO(Mensagem m) {
        return toResponseDTO(m, m.getAcordo().getId());
    }

    private MensagemResponseDTO toResponseDTO(Mensagem m, Long acordoId) {
        return new MensagemResponseDTO(
                m.getId(),
                acordoId,
                m.getRemetente().getId(),
                m.getRemetente().getNome(),
                m.getConteudo(),
                m.getEnviadoEm()
        );
    }
}
