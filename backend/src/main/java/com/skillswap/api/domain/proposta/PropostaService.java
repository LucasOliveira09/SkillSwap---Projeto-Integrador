package com.skillswap.api.domain.proposta;

import com.skillswap.api.domain.acordo.Acordo;
import com.skillswap.api.domain.mensagem.Mensagem;
import com.skillswap.api.domain.mensagem.MensagemRepository;
import com.skillswap.api.domain.acordo.AcordoRepository;
import com.skillswap.api.domain.anuncio.Anuncio;
import com.skillswap.api.domain.anuncio.AnuncioRepository;
import com.skillswap.api.domain.anuncio.AnuncioService;
import com.skillswap.api.domain.anuncio.StatusAnuncio;
import com.skillswap.api.domain.notificacao.NotificacaoService;
import com.skillswap.api.domain.notificacao.TipoNotificacao;
import com.skillswap.api.domain.acordo.EventoAcordoService;
import com.skillswap.api.domain.acordo.TipoEventoAcordo;
import com.skillswap.api.domain.conversa.ConversaService;
import com.skillswap.api.domain.proposta.dto.PropostaRequestDTO;
import com.skillswap.api.domain.proposta.dto.PropostaResponseDTO;
import com.skillswap.api.domain.proposta.dto.AtualizarStatusPropostaDTO;
import com.skillswap.api.domain.proposta.dto.PropostaConversaRequestDTO;
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
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class PropostaService {
    private static final ZoneId ZONA_PRODUTO = ZoneId.of("America/Sao_Paulo");

    private final MensagemRepository mensagens;
    private final PropostaRepository propostaRepository;
    private final AnuncioService anuncioService;
    private final AnuncioRepository anuncioRepository;
    private final UsuarioService usuarioService;
    private final AcordoRepository acordoRepository;
    private final NotificacaoService notificacaoService;
    private final EventoAcordoService eventoAcordoService;
    private final ConversaService conversaService;

    @Transactional
    public Proposta criarNaConversa(Long conversaId, PropostaConversaRequestDTO dto, String emailUsuario) {
        var conversa = conversaService.exigirParticipante(conversaId, emailUsuario);
        var proponente = usuarioService.findByEmail(emailUsuario);
        var destinatario = conversaService.outroParticipante(conversa, emailUsuario);
        var anuncio = anuncioRepository.findByIdForUpdate(dto.anuncioId())
                .filter(a -> a.getStatus() == StatusAnuncio.ATIVO && a.getUsuario().getId().equals(destinatario.getId())
                        && a.getUsuario().isPerfilPublico() && !a.isExemplo())
                .orElseThrow(() -> new NaoEncontradoException("Anúncio do destinatário indisponível"));

        validarTermos(dto, anuncio.getModalidade());
        boolean jaProposto = propostaRepository.existsByAnuncioIdAndProponenteIdAndStatusIn(
                anuncio.getId(), proponente.getId(), Set.of(StatusProposta.PENDENTE, StatusProposta.ACEITA));
        if (jaProposto) throw new IllegalArgumentException("Você já possui uma proposta ativa para este anúncio");

        var proposta = propostaRepository.save(Proposta.builder()
                .conversa(conversa).anuncio(anuncio).proponente(proponente)
                .mensagemInicial(dto.servico().strip()).servico(dto.servico().strip())
                .escopo(dto.escopo().strip()).formaPagamento(dto.formaPagamento())
                .valor(dto.valor()).parcelas(dto.parcelas()).duracao(normalizar(dto.duracao()))
                .validadeEm(dto.validadeEm()).build());
        String resumo = "Proposta enviada: " + proposta.getServico() + " — " +
                (proposta.getFormaPagamento() == FormaPagamentoProposta.TROCA ? "troca sem complemento em dinheiro"
                        : "R$ " + proposta.getValor().toPlainString() +
                        (proposta.getFormaPagamento() == FormaPagamentoProposta.PARCELADO ? " em " + proposta.getParcelas() + " parcelas" : " à vista"));
        mensagens.save(Mensagem.builder().conversa(conversa).anuncioContexto(anuncio)
                .proposta(proposta).remetente(proponente).conteudo(resumo).build());
        notificacaoService.criar(destinatario, TipoNotificacao.PROPOSTA_RECEBIDA, "Nova proposta",
                proponente.getNome() + " enviou uma proposta para “" + anuncio.getTitulo() + "”.",
                "/mensagens?conversa=chat-" + conversa.getId());
        return proposta;
    }

    private void validarTermos(PropostaConversaRequestDTO dto, com.skillswap.api.domain.anuncio.ModalidadeAnuncio modalidade) {
        if (dto.validadeEm() != null && dto.validadeEm().isBefore(LocalDate.now(ZONA_PRODUTO)))
            throw new IllegalArgumentException("A validade da proposta não pode estar no passado");
        if (modalidade == com.skillswap.api.domain.anuncio.ModalidadeAnuncio.TROCA) {
            if (dto.formaPagamento() != FormaPagamentoProposta.TROCA || dto.valor() != null || dto.parcelas() != null)
                throw new IllegalArgumentException("Anúncios de troca não aceitam complemento em dinheiro");
        } else if (dto.formaPagamento() == FormaPagamentoProposta.TROCA) {
            throw new IllegalArgumentException("A forma de pagamento por troca só vale para anúncios de troca");
        } else if (dto.valor() == null || dto.valor().signum() <= 0) {
            throw new IllegalArgumentException("Informe um valor maior que zero");
        } else if (dto.formaPagamento() == FormaPagamentoProposta.A_VISTA && dto.parcelas() != null) {
            throw new IllegalArgumentException("Pagamento à vista não deve informar parcelas");
        } else if (dto.formaPagamento() == FormaPagamentoProposta.PARCELADO &&
                (dto.parcelas() == null || dto.parcelas() < 2 || dto.parcelas() > 12)) {
            throw new IllegalArgumentException("O parcelamento deve ter entre 2 e 12 parcelas");
        }
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

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

        var conversa = conversaService.obterOuCriar(anuncio.getUsuario(), proponente).conversa();
        var proposta = Proposta.builder()
                .conversa(conversa)
                .anuncio(anuncio)
                .proponente(proponente)
                .mensagemInicial(dto.mensagemInicial())
                .build();
        proposta = propostaRepository.save(proposta);
        mensagens.save(Mensagem.builder().conversa(conversa).anuncioContexto(anuncio)
                .proposta(proposta).remetente(proponente).conteudo(dto.mensagemInicial().strip()).build());
        notificacaoService.criar(anuncio.getUsuario(), TipoNotificacao.PROPOSTA_RECEBIDA,
                "Nova proposta", proponente.getNome() + " demonstrou interesse em “" + anuncio.getTitulo() + "”.",
                "/mensagens?conversa=chat-" + conversa.getId());
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
        if (proposta.getValidadeEm() != null && proposta.getValidadeEm().isBefore(LocalDate.now(ZONA_PRODUTO)))
            throw new IllegalArgumentException("A validade da proposta expirou; solicite uma nova versão");

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
        mensagens.associarAcordo(proposta.getId(), acordo);
        eventoAcordoService.registrar(acordo, proposta.getAnuncio().getUsuario(), TipoEventoAcordo.ACORDO_INICIADO);
        notificacaoService.criar(proposta.getProponente(), TipoNotificacao.PROPOSTA_RESPONDIDA,
                "Proposta aceita", "Sua proposta para “" + proposta.getAnuncio().getTitulo() + "” foi aceita.",
                linkConversa(proposta));

        // Recusar outras propostas pendentes do mesmo anúncio
        propostasDoAnuncio.stream()
                .filter(p -> !p.getId().equals(propostaId) && p.getStatus() == StatusProposta.PENDENTE)
                .forEach(p -> {
                    p.setStatus(StatusProposta.RECUSADA);
                    propostaRepository.save(p);
                    notificacaoService.criar(p.getProponente(), TipoNotificacao.PROPOSTA_RESPONDIDA,
                            "Proposta encerrada", "Outra proposta foi aceita para “" + p.getAnuncio().getTitulo() + "”.",
                            linkConversa(p));
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
                linkConversa(proposta));
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
                linkConversa(proposta));
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

    private String linkConversa(Proposta proposta) {
        return proposta.getConversa() == null
                ? "/conta?aba=enviadas&propostaId=" + proposta.getId()
                : "/mensagens?conversa=chat-" + proposta.getConversa().getId();
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
                p.getCriadoEm(),
                p.getServico(),
                p.getEscopo(),
                p.getFormaPagamento(),
                p.getValor(),
                p.getParcelas(),
                p.getDuracao(),
                p.getValidadeEm()
        );
    }
}
