package com.skillswap.api.domain.avaliacao;

import com.skillswap.api.domain.acordo.Acordo;
import com.skillswap.api.domain.acordo.AcordoService;
import com.skillswap.api.domain.acordo.StatusAcordo;
import com.skillswap.api.domain.avaliacao.dto.AvaliacaoRequestDTO;
import com.skillswap.api.domain.avaliacao.dto.AvaliacaoResponseDTO;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final AcordoService acordoService;
    private final UsuarioService usuarioService;

    @Transactional
    public Avaliacao criar(Long acordoId, AvaliacaoRequestDTO dto, String emailUsuario) {
        Acordo acordo = acordoService.findById(acordoId);

        if (acordo.getStatus() != StatusAcordo.CONCLUIDO) {
            throw new IllegalArgumentException("Acordo não está concluído");
        }

        Usuario avaliador = usuarioService.findByEmail(emailUsuario);

        // Verificar se é participante
        String emailAnunciante = acordo.getProposta().getAnuncio().getUsuario().getEmail();
        String emailProponente = acordo.getProposta().getProponente().getEmail();
        if (!emailAnunciante.equals(emailUsuario) && !emailProponente.equals(emailUsuario)) {
            throw new NaoAutorizadoException("Você não é participante deste acordo");
        }

        // Determinar avaliado (o outro participante)
        Usuario avaliado;
        if (emailAnunciante.equals(emailUsuario)) {
            avaliado = acordo.getProposta().getProponente();
        } else {
            avaliado = acordo.getProposta().getAnuncio().getUsuario();
        }

        // Sem autoavaliação
        if (avaliador.getId().equals(avaliado.getId())) {
            throw new IllegalArgumentException("Você não pode avaliar a si mesmo");
        }

        // Sem duplicação
        if (avaliacaoRepository.existsByAcordoIdAndAvaliadorId(acordoId, avaliador.getId())) {
            throw new IllegalArgumentException("Você já avaliou este acordo");
        }

        var avaliacao = Avaliacao.builder()
                .acordo(acordo)
                .avaliador(avaliador)
                .avaliado(avaliado)
                .nota(dto.nota())
                .depoimento(dto.depoimento())
                .build();
        return avaliacaoRepository.save(avaliacao);
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponseDTO> listarPorUsuario(Long usuarioId) {
        return avaliacaoRepository.findByAvaliadoId(usuarioId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public AvaliacaoResponseDTO toResponseDTO(Avaliacao a) {
        return new AvaliacaoResponseDTO(
                a.getId(),
                a.getAcordo().getId(),
                a.getAvaliador().getId(),
                a.getAvaliador().getNome(),
                a.getAvaliado().getId(),
                a.getAvaliado().getNome(),
                a.getNota(),
                a.getDepoimento(),
                a.getCriadoEm()
        );
    }
}
