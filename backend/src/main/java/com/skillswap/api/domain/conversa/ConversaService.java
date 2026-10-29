package com.skillswap.api.domain.conversa;

import com.skillswap.api.domain.proposta.*;
import com.skillswap.api.domain.proposta.dto.PropostaRequestDTO;
import com.skillswap.api.domain.mensagem.*;
import com.skillswap.api.domain.acordo.*;
import com.skillswap.api.domain.anuncio.*;
import com.skillswap.api.domain.usuario.*;
import com.skillswap.api.domain.notificacao.*;
import com.skillswap.api.infra.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversaService {
    private final PropostaRepository propostas;
    private final PropostaService propostaService;
    private final AnuncioRepository anuncios;
    private final MensagemRepository mensagens;
    private final AcordoRepository acordos;
    private final UsuarioService usuarios;
    private final NotificacaoService notificacoes;

    public record Abertura(ConversaDTO conversa, boolean criada) {}

    @Transactional
    public Abertura abrir(Long anuncioId, String conteudo, String email) {
        var usuario = usuarios.findByEmail(email);
        var anuncio = anuncios.findByIdForUpdate(anuncioId)
                .filter(a -> a.getStatus() == StatusAnuncio.ATIVO && a.getUsuario().isPerfilPublico())
                .orElseThrow(() -> new NaoEncontradoException("Anuncio indisponivel"));
        if (anuncio.isExemplo()) throw new IllegalArgumentException("Anuncios ilustrativos nao recebem mensagens");
        if (anuncio.getUsuario().getId().equals(usuario.getId()))
            throw new IllegalArgumentException("Voce nao pode enviar mensagem ao proprio anuncio");
        var existente = propostas.findByAnuncioIdForUpdate(anuncioId).stream()
                .filter(p -> p.getProponente().getId().equals(usuario.getId()))
                .filter(p -> p.getStatus() == StatusProposta.PENDENTE || p.getStatus() == StatusProposta.ACEITA)
                .findFirst();
        if (existente.isPresent()) {
            enviar(existente.get().getId(), conteudo, email);
            return new Abertura(detalhar(existente.get(), usuario.getId()), false);
        }
        var proposta = propostaService.criar(anuncioId, new PropostaRequestDTO(conteudo.strip()), email);
        return new Abertura(detalhar(proposta, usuario.getId()), true);
    }

    @Transactional(readOnly = true)
    public Page<ConversaDTO> listar(String email, Pageable pageable) {
        var usuario = usuarios.findByEmail(email);
        var pagina = propostas.listarConversas(usuario.getId(), pageable);
        var detalhes = detalhes(pagina.getContent(), usuario.getId());
        return pagina.map(p -> detalhes.get(p.getId()));
    }

    @Transactional(readOnly = true)
    public ConversaDTO buscar(Long id, String email) {
        var usuario = usuarios.findByEmail(email);
        return detalhar(participante(id, usuario, false), usuario.getId());
    }

    @Transactional(readOnly = true)
    public long naoLidas(String email) { return mensagens.contarNaoLidas(usuarios.findByEmail(email).getId()); }

    @Transactional(readOnly = true)
    public Page<MensagemConversaDTO> listarMensagens(Long id, String email, Pageable pageable) {
        participante(id, usuarios.findByEmail(email), false);
        return mensagens.findByPropostaId(id, pageable).map(MensagemConversaDTO::de);
    }

    @Transactional
    public MensagemConversaDTO enviar(Long id, String conteudo, String email) {
        var usuario = usuarios.findByEmail(email);
        var proposta = participante(id, usuario, true);
        var acordo = acordos.findByPropostaId(id).map(a -> acordos.findByIdForUpdate(a.getId()).orElseThrow()).orElse(null);
        if (!podeEnviar(proposta, acordo)) throw new IllegalArgumentException("Esta conversa foi encerrada");
        var mensagem = mensagens.save(Mensagem.builder().proposta(proposta).acordo(acordo)
                .remetente(usuario).conteudo(conteudo.strip()).build());
        var destinatario = proposta.getProponente().getId().equals(usuario.getId())
                ? proposta.getAnuncio().getUsuario() : proposta.getProponente();
        notificacoes.criar(destinatario, TipoNotificacao.NOVA_MENSAGEM, "Nova mensagem",
                usuario.getNome() + " enviou uma mensagem sobre “" + proposta.getAnuncio().getTitulo() + "”.",
                "/mensagens?conversa=" + id);
        return MensagemConversaDTO.de(mensagem);
    }

    @Transactional
    public void ler(Long id, Long ultimaMensagemId, String email) {
        var usuario = usuarios.findByEmail(email);
        var proposta = participante(id, usuario, true);
        var mensagem = mensagens.findByIdAndPropostaId(ultimaMensagemId, id)
                .orElseThrow(() -> new NaoEncontradoException("Mensagem nao encontrada"));
        boolean proponente = proposta.getProponente().getId().equals(usuario.getId());
        var dataAtual = proponente ? proposta.getUltimaLeituraProponenteEm() : proposta.getUltimaLeituraAnuncianteEm();
        long idAtual = proponente ? proposta.getUltimaLidaProponente() : proposta.getUltimaLidaAnunciante();
        if (dataAtual == null || mensagem.getEnviadoEm().isAfter(dataAtual)
                || (mensagem.getEnviadoEm().equals(dataAtual) && ultimaMensagemId > idAtual)) {
            if (proponente) {
                proposta.setUltimaLidaProponente(ultimaMensagemId);
                proposta.setUltimaLeituraProponenteEm(mensagem.getEnviadoEm());
            } else {
                proposta.setUltimaLidaAnunciante(ultimaMensagemId);
                proposta.setUltimaLeituraAnuncianteEm(mensagem.getEnviadoEm());
            }
        }
    }

    @Transactional(readOnly = true)
    public MensagemConversaDTO buscarMensagem(Long conversaId, Long mensagemId, String email) {
        participante(conversaId, usuarios.findByEmail(email), false);
        return mensagens.findByIdAndPropostaId(mensagemId, conversaId).map(MensagemConversaDTO::de)
                .orElseThrow(() -> new NaoEncontradoException("Mensagem nao encontrada"));
    }

    private Proposta participante(Long id, Usuario usuario, boolean bloquear) {
        var proposta = (bloquear ? propostas.findByIdForUpdate(id) : propostas.findById(id))
                .orElseThrow(() -> new NaoEncontradoException("Conversa nao encontrada"));
        if (!proposta.getProponente().getId().equals(usuario.getId()) &&
                !proposta.getAnuncio().getUsuario().getId().equals(usuario.getId()))
            throw new NaoAutorizadoException("Voce nao participa desta conversa");
        return proposta;
    }

    private boolean podeEnviar(Proposta p, Acordo acordo) {
        return (p.getStatus() == StatusProposta.PENDENTE || p.getStatus() == StatusProposta.ACEITA)
                && (acordo == null || acordo.getStatus() == StatusAcordo.EM_ANDAMENTO);
    }

    private ConversaDTO detalhar(Proposta p, Long usuarioId) { return detalhes(List.of(p), usuarioId).get(p.getId()); }

    private Map<Long, ConversaDTO> detalhes(List<Proposta> itens, Long usuarioId) {
        if (itens.isEmpty()) return Map.of();
        var ids = itens.stream().map(Proposta::getId).toList();
        var ultimas = mensagens.ultimas(ids).stream().collect(Collectors.toMap(m -> m.getProposta().getId(), Function.identity()));
        var naoLidas = mensagens.contarPorConversa(ids, usuarioId).stream().collect(Collectors.toMap(MensagemRepository.Contagem::getPropostaId, MensagemRepository.Contagem::getQuantidade));
        var porProposta = acordos.findByPropostaIdIn(ids).stream().collect(Collectors.toMap(a -> a.getProposta().getId(), Function.identity()));
        var resultado = new HashMap<Long, ConversaDTO>();
        for (var p : itens) {
            var a = p.getAnuncio();
            var acordo = porProposta.get(p.getId());
            var ultima = ultimas.get(p.getId());
            resultado.put(p.getId(), new ConversaDTO(p.getId(), a.getId(), a.getTitulo(),
                    a.getStatus() == StatusAnuncio.ATIVO && a.getUsuario().isPerfilPublico() ? a.getImagem() : null,
                    a.getCategoria().name(), a.getCidade(), a.getModalidade().name(), a.getPreco(),
                    a.getUsuario().getId(), a.getUsuario().getNome(), p.getProponente().getId(), p.getProponente().getNome(),
                    p.getStatus(), acordo == null ? null : acordo.getId(), acordo == null ? null : acordo.getStatus(),
                    podeEnviar(p, acordo), ultima == null ? null : MensagemConversaDTO.de(ultima),
                    naoLidas.getOrDefault(p.getId(), 0L), p.getCriadoEm()));
        }
        return resultado;
    }
}
