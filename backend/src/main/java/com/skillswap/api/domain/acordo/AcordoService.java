package com.skillswap.api.domain.acordo;

import com.skillswap.api.domain.acordo.dto.AcordoResponseDTO;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AcordoService {

    private final AcordoRepository acordoRepository;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<AcordoResponseDTO> listarMeus(String emailUsuario) {
        Usuario usuario = usuarioService.findByEmail(emailUsuario);
        return acordoRepository.findByPropostaAnuncioUsuarioIdOrPropostaProponenteId(
                        usuario.getId(), usuario.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public Acordo concluir(Long id, String emailUsuario) {
        Acordo acordo = findById(id);
        checkParticipant(acordo, emailUsuario);
        if (acordo.getStatus() != StatusAcordo.EM_ANDAMENTO) {
            throw new IllegalArgumentException("Acordo não está em andamento");
        }
        acordo.setStatus(StatusAcordo.CONCLUIDO);
        acordo.setConcluidoEm(LocalDateTime.now());
        return acordoRepository.save(acordo);
    }

    @Transactional
    public Acordo cancelar(Long id, String emailUsuario) {
        Acordo acordo = findById(id);
        checkParticipant(acordo, emailUsuario);
        if (acordo.getStatus() != StatusAcordo.EM_ANDAMENTO) {
            throw new IllegalArgumentException("Acordo não está em andamento");
        }
        acordo.setStatus(StatusAcordo.CANCELADO);
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
                a.getConcluidoEm()
        );
    }
}
