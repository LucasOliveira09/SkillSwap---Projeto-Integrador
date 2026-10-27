package com.skillswap.api.domain.proposta;

import com.skillswap.api.domain.acordo.Acordo;
import com.skillswap.api.domain.acordo.AcordoRepository;
import com.skillswap.api.domain.anuncio.Anuncio;
import com.skillswap.api.domain.anuncio.AnuncioRepository;
import com.skillswap.api.domain.anuncio.AnuncioService;
import com.skillswap.api.domain.anuncio.StatusAnuncio;
import com.skillswap.api.domain.proposta.dto.PropostaRequestDTO;
import com.skillswap.api.domain.proposta.dto.PropostaResponseDTO;
import com.skillswap.api.domain.proposta.dto.AtualizarStatusPropostaDTO;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PropostaService {

    private final PropostaRepository propostaRepository;
    private final AnuncioService anuncioService;
    private final AnuncioRepository anuncioRepository;
    private final UsuarioService usuarioService;
    private final AcordoRepository acordoRepository;

    @Transactional
    public Proposta criar(Long anuncioId, PropostaRequestDTO dto, String emailUsuario) {
        anuncioService.buscarPorId(anuncioId);
        Anuncio anuncio = anuncioRepository.findByIdForUpdate(anuncioId)
                .filter(item -> item.getStatus() == StatusAnuncio.ATIVO)
                .orElseThrow(() -> new NaoEncontradoException("Anúncio não encontrado"));
        Usuario proponente = usuarioService.findByEmail(emailUsuario);

        if (anuncio.getUsuario().getId().equals(proponente.getId())) {
            throw new IllegalArgumentException("Você não pode fazer proposta no próprio anúncio");
        }

        boolean jaProposto = propostaRepository.existsByAnuncioIdAndProponenteIdAndStatusIn(
                anuncioId, proponente.getId(), Set.of(StatusProposta.PENDENTE, StatusProposta.ACEITA));
        if (jaProposto) {
            throw new IllegalArgumentException("Você já possui uma proposta ativa para este anúncio");
        }

        var proposta = Proposta.builder()
                .anuncio(anuncio)
                .proponente(proponente)
                .mensagemInicial(dto.mensagemInicial())
                .build();
        return propostaRepository.save(proposta);
    }

    @Transactional(readOnly = true)
    public List<PropostaResponseDTO> listarRecebidas(String emailUsuario) {
        Usuario usuario = usuarioService.findByEmail(emailUsuario);
        return propostaRepository.findByAnuncioUsuarioId(usuario.getId()).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PropostaResponseDTO> listarEnviadas(String emailUsuario) {
        Usuario usuario = usuarioService.findByEmail(emailUsuario);
        return propostaRepository.findByProponenteId(usuario.getId()).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PropostaResponseDTO> listarPorDirecao(String direcao, String emailUsuario, Pageable pageable) {
        Usuario usuario = usuarioService.findByEmail(emailUsuario);
        return switch (direcao == null ? "" : direcao.toUpperCase(java.util.Locale.ROOT)) {
            case "ENVIADAS" -> propostaRepository.findByProponenteId(usuario.getId(), pageable).map(this::toResponseDTO);
            case "RECEBIDAS" -> propostaRepository.findByAnuncioUsuarioId(usuario.getId(), pageable).map(this::toResponseDTO);
            default -> throw new IllegalArgumentException("Direção deve ser ENVIADAS ou RECEBIDAS");
        };
    }

    @Transactional(readOnly = true)
    public PropostaResponseDTO buscar(Long id, String emailUsuario) {
        Proposta proposta = findById(id);
        if (!proposta.getAnuncio().getUsuario().getEmail().equals(emailUsuario)
                && !proposta.getProponente().getEmail().equals(emailUsuario)) {
            throw new NaoAutorizadoException("Você não participa desta proposta");
        }
        return toResponseDTO(proposta);
    }

    @Transactional
    public Proposta atualizarStatus(Long id, AtualizarStatusPropostaDTO dto, String emailUsuario) {
        return switch (dto.status()) {
            case ACEITA -> aceitar(id, emailUsuario);
            case RECUSADA -> recusar(id, emailUsuario);
            case CANCELADA -> cancelar(id, emailUsuario);
            default -> throw new IllegalArgumentException("Status solicitado não é permitido nesta operação");
        };
    }

    @Transactional
    public Proposta aceitar(Long propostaId, String emailUsuario) {
        Proposta proposta = findPorIdComAnuncioBloqueado(propostaId);
        checkAnuncioOwner(proposta, emailUsuario);

        if (proposta.getStatus() != StatusProposta.PENDENTE) {
            throw new IllegalArgumentException("Proposta não está pendente");
        }

        proposta.setStatus(StatusProposta.ACEITA);
        propostaRepository.save(proposta);

        // Recusar outras propostas pendentes do mesmo anúncio
        propostaRepository.findByAnuncioId(proposta.getAnuncio().getId()).stream()
                .filter(p -> !p.getId().equals(propostaId) && p.getStatus() == StatusProposta.PENDENTE)
                .forEach(p -> {
                    p.setStatus(StatusProposta.RECUSADA);
                    propostaRepository.save(p);
                });

        // Criar acordo
        var acordo = Acordo.builder()
                .proposta(proposta)
                .build();
        acordoRepository.save(acordo);

        return proposta;
    }

    @Transactional
    public Proposta recusar(Long propostaId, String emailUsuario) {
        Proposta proposta = findPorIdComAnuncioBloqueado(propostaId);
        checkAnuncioOwner(proposta, emailUsuario);

        if (proposta.getStatus() != StatusProposta.PENDENTE) {
            throw new IllegalArgumentException("Proposta não está pendente");
        }

        proposta.setStatus(StatusProposta.RECUSADA);
        return propostaRepository.save(proposta);
    }

    @Transactional
    public Proposta cancelar(Long propostaId, String emailUsuario) {
        Proposta proposta = findPorIdComAnuncioBloqueado(propostaId);
        if (!proposta.getProponente().getEmail().equals(emailUsuario)) {
            throw new NaoAutorizadoException("Você não enviou esta proposta");
        }
        if (proposta.getStatus() != StatusProposta.PENDENTE) {
            throw new IllegalArgumentException("Somente propostas pendentes podem ser canceladas");
        }
        proposta.setStatus(StatusProposta.CANCELADA);
        return propostaRepository.save(proposta);
    }

    private Proposta findPorIdComAnuncioBloqueado(Long propostaId) {
        Long anuncioId = propostaRepository.findAnuncioIdById(propostaId)
                .orElseThrow(() -> new NaoEncontradoException("Proposta não encontrada"));
        anuncioRepository.findByIdForUpdate(anuncioId)
                .orElseThrow(() -> new NaoEncontradoException("Anúncio não encontrado"));
        return findById(propostaId);
    }

    public Proposta findById(Long id) {
        return propostaRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Proposta não encontrada"));
    }

    private void checkAnuncioOwner(Proposta proposta, String emailUsuario) {
        if (!proposta.getAnuncio().getUsuario().getEmail().equals(emailUsuario)) {
            throw new NaoAutorizadoException("Você não é o dono deste anúncio");
        }
    }

    public PropostaResponseDTO toResponseDTO(Proposta p) {
        return new PropostaResponseDTO(
                p.getId(),
                p.getAnuncio().getId(),
                p.getAnuncio().getTitulo(),
                p.getProponente().getId(),
                p.getProponente().getNome(),
                p.getMensagemInicial(),
                p.getStatus(),
                p.getCriadoEm()
        );
    }
}
