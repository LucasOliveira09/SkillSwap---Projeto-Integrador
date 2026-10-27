package com.skillswap.api.domain.mensagem;

import com.skillswap.api.domain.acordo.Acordo;
import com.skillswap.api.domain.acordo.AcordoService;
import com.skillswap.api.domain.mensagem.dto.MensagemRequestDTO;
import com.skillswap.api.domain.mensagem.dto.MensagemResponseDTO;
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

    @Transactional
    public Mensagem enviar(Long acordoId, MensagemRequestDTO dto, String emailUsuario) {
        Acordo acordo = acordoService.findById(acordoId);
        checkParticipant(acordo, emailUsuario);
        Usuario remetente = usuarioService.findByEmail(emailUsuario);

        var mensagem = Mensagem.builder()
                .acordo(acordo)
                .remetente(remetente)
                .conteudo(dto.conteudo())
                .build();
        return mensagemRepository.save(mensagem);
    }

    @Transactional(readOnly = true)
    public List<MensagemResponseDTO> listarPorAcordo(Long acordoId, String emailUsuario) {
        Acordo acordo = acordoService.findById(acordoId);
        checkParticipant(acordo, emailUsuario);
        return mensagemRepository.findByAcordoIdOrderByEnviadoEmAsc(acordoId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<MensagemResponseDTO> listarPorAcordo(Long acordoId, String emailUsuario, Pageable pageable) {
        Acordo acordo = acordoService.findById(acordoId);
        checkParticipant(acordo, emailUsuario);
        return mensagemRepository.findByAcordoId(acordoId, pageable).map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public MensagemResponseDTO buscar(Long acordoId, Long mensagemId, String emailUsuario) {
        Acordo acordo = acordoService.findById(acordoId);
        checkParticipant(acordo, emailUsuario);
        return mensagemRepository.findByIdAndAcordoId(mensagemId, acordoId)
                .map(this::toResponseDTO)
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
        return new MensagemResponseDTO(
                m.getId(),
                m.getAcordo().getId(),
                m.getRemetente().getId(),
                m.getRemetente().getNome(),
                m.getConteudo(),
                m.getEnviadoEm()
        );
    }
}
