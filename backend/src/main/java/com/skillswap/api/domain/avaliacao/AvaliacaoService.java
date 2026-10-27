package com.skillswap.api.domain.avaliacao;

import com.skillswap.api.domain.acordo.Acordo;
import com.skillswap.api.domain.acordo.AcordoService;
import com.skillswap.api.domain.acordo.StatusAcordo;
import com.skillswap.api.domain.avaliacao.dto.AvaliacaoRequestDTO;
import com.skillswap.api.domain.avaliacao.dto.AvaliacaoResponseDTO;
import com.skillswap.api.domain.avaliacao.dto.RespostaAvaliacaoDTO;
import com.skillswap.api.infra.exception.ConflitoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

        if (!emailProponente.equals(emailUsuario)) {
            throw new NaoAutorizadoException("Somente o cliente que enviou a proposta pode avaliar o profissional e o serviço");
        }

        // Determinar avaliado (o outro participante)
        Usuario avaliado = acordo.getProposta().getAnuncio().getUsuario();

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
                .notaAnuncio(dto.notaAnuncio())
                .depoimento(dto.depoimento())
                .build();
        return avaliacaoRepository.save(avaliacao);
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponseDTO> listarPorUsuario(Long usuarioId) {
        return avaliacaoRepository.findByAvaliadoIdAndModeradaOcultaFalse(usuarioId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponseDTO> listarPorAcordo(Long acordoId, String emailUsuario) {
        Acordo acordo = acordoService.findById(acordoId);
        String anunciante = acordo.getProposta().getAnuncio().getUsuario().getEmail();
        String proponente = acordo.getProposta().getProponente().getEmail();
        if (!anunciante.equals(emailUsuario) && !proponente.equals(emailUsuario)) {
            throw new NaoAutorizadoException("Você não participa deste acordo");
        }
        return avaliacaoRepository.findByAcordoId(acordoId).stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public AvaliacaoResponseDTO buscarPorAcordo(Long acordoId, Long avaliacaoId, String emailUsuario) {
        listarPorAcordo(acordoId, emailUsuario);
        return avaliacaoRepository.findByIdAndAcordoId(avaliacaoId, acordoId)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new com.skillswap.api.infra.exception.NaoEncontradoException("Avaliação não encontrada"));
    }

    @Transactional
    public AvaliacaoResponseDTO responder(Long acordoId, Long avaliacaoId, RespostaAvaliacaoDTO dto, String emailUsuario) {
        Acordo acordo = acordoService.findById(acordoId);
        String anunciante = acordo.getProposta().getAnuncio().getUsuario().getEmail();
        String proponente = acordo.getProposta().getProponente().getEmail();
        if (!anunciante.equalsIgnoreCase(emailUsuario) && !proponente.equalsIgnoreCase(emailUsuario)) {
            throw new NaoAutorizadoException("Você não participa deste acordo");
        }
        if (acordo.getStatus() != StatusAcordo.CONCLUIDO) {
            throw new ConflitoException("A avaliação só pode receber resposta após a conclusão do acordo.");
        }
        Avaliacao avaliacao = avaliacaoRepository.findByIdAndAcordoIdForUpdate(avaliacaoId, acordoId)
                .orElseThrow(() -> new NaoEncontradoException("Avaliação não encontrada"));
        Usuario profissional = usuarioService.findByEmail(emailUsuario);
        if (!avaliacao.getAvaliado().getId().equals(profissional.getId())) {
            throw new NaoAutorizadoException("Somente o profissional avaliado pode responder");
        }
        if (avaliacao.isModeradaOculta()) {
            throw new ConflitoException("Não é possível responder a uma avaliação ocultada pela moderação.");
        }
        if (avaliacao.getRespostaProfissional() != null) {
            throw new ConflitoException("Esta avaliação já recebeu uma resposta.");
        }
        avaliacao.setRespostaProfissional(dto.resposta().strip());
        avaliacao.setRespondidaEm(LocalDateTime.now());
        return toResponseDTO(avaliacaoRepository.save(avaliacao));
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
                a.getNotaAnuncio(),
                a.getDepoimento(),
                a.getCriadoEm(),
                a.getRespostaProfissional(),
                a.getRespondidaEm()
        );
    }
}
