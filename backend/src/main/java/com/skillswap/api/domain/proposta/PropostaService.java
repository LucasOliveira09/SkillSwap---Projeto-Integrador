package com.skillswap.api.domain.proposta;

import com.skillswap.api.domain.acordo.Acordo;
import com.skillswap.api.domain.acordo.AcordoRepository;
import com.skillswap.api.domain.anuncio.Anuncio;
import com.skillswap.api.domain.anuncio.AnuncioRepository;
import com.skillswap.api.domain.anuncio.AnuncioService;
import com.skillswap.api.domain.anuncio.StatusAnuncio;
import com.skillswap.api.domain.notificacao.NotificacaoService;
import com.skillswap.api.domain.notificacao.TipoNotificacao;
import com.skillswap.api.domain.acordo.EventoAcordoService;
import com.skillswap.api.domain.acordo.TipoEventoAcordo;
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
    private final NotificacaoService notificacaoService;
    private final EventoAcordoService eventoAcordoService;

    @Transactional
    public Proposta criar(Long anuncioId, PropostaRequestDTO dto, String emailUsuario) {
        anuncioService.buscarPorId(anuncioId);
        Anuncio anuncio = anuncioRepository.findByIdForUpdate(anuncioId)
                .filter(item -> item.getStatus() == StatusAnuncio.ATIVO && item.getUsuario().isPerfilPublico())
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
        proposta = propostaRepository.save(proposta);
        notificacaoService.criar(anuncio.getUsuario(), TipoNotificacao.PROPOSTA_RECEBIDA,
                "Nova proposta", proponente.getNome() + " demonstrou interesse em “" + anuncio.getTitulo() + "”.",
                "/conta?aba=recebidas&propostaId=" + proposta.getId());
        return proposta;
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

        List<Proposta> propostasDoAnuncio = propostaRepository.findByAnuncioIdForUpdate(proposta.getAnuncio().getId());
        boolean outraJaAceita = propostasDoAnuncio.stream()
                .anyMatch(item -> !item.getId().equals(propostaId) && item.getStatus() == StatusProposta.ACEITA);
        if (outraJaAceita) {
            throw new IllegalArgumentException("Já existe uma proposta aceita para este anúncio");
        }

        proposta.setStatus(StatusProposta.ACEITA);
        propostaRepository.save(proposta);

        // Avisa a pessoa que enviou a proposta e aponta para a nova conversa.
        var acordo = Acordo.builder().proposta(proposta).build();
        acordoRepository.save(acordo);
        eventoAcordoService.registrar(acordo, proposta.getAnuncio().getUsuario(), TipoEventoAcordo.ACORDO_INICIADO);
        notificacaoService.criar(proposta.getProponente(), TipoNotificacao.PROPOSTA_RESPONDIDA,
                "Proposta aceita", "Sua proposta para “" + proposta.getAnuncio().getTitulo() + "” foi aceita.",
                "/acordos/" + acordo.getId());

        // Recusar outras propostas pendentes do mesmo anúncio
        propostasDoAnuncio.stream()
                .filter(p -> !p.getId().equals(propostaId) && p.getStatus() == StatusProposta.PENDENTE)
                .forEach(p -> {
                    p.setStatus(StatusProposta.RECUSADA);
                    propostaRepository.save(p);
                    notificacaoService.criar(p.getProponente(), TipoNotificacao.PROPOSTA_RESPONDIDA,
                            "Proposta encerrada", "Outra proposta foi aceita para “" + p.getAnuncio().getTitulo() + "”.",
                            "/conta?aba=enviadas&propostaId=" + p.getId());
                });

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
        notificacaoService.criar(proposta.getProponente(), TipoNotificacao.PROPOSTA_RESPONDIDA,
                "Proposta recusada", "Sua proposta para “" + proposta.getAnuncio().getTitulo() + "” foi recusada.",
                "/conta?aba=enviadas&propostaId=" + proposta.getId());
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
        notificacaoService.criar(proposta.getAnuncio().getUsuario(), TipoNotificacao.PROPOSTA_RESPONDIDA,
                "Proposta cancelada", proposta.getProponente().getNome() + " cancelou a proposta para “" + proposta.getAnuncio().getTitulo() + "”.",
                "/conta?aba=recebidas&propostaId=" + proposta.getId());
        return propostaRepository.save(proposta);
    }

    private Proposta findPorIdComAnuncioBloqueado(Long propostaId) {
        Long anuncioId = propostaRepository.findAnuncioIdById(propostaId)
                .orElseThrow(() -> new NaoEncontradoException("Proposta não encontrada"));
        anuncioRepository.findByIdForUpdate(anuncioId)
                .orElseThrow(() -> new NaoEncontradoException("Anúncio não encontrado"));
        return propostaRepository.findByAnuncioIdForUpdate(anuncioId).stream()
                .filter(proposta -> proposta.getId().equals(propostaId))
                .findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Proposta não encontrada"));
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
