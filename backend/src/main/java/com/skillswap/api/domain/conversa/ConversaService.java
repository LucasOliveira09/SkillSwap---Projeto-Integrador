package com.skillswap.api.domain.conversa;

import com.skillswap.api.domain.acordo.StatusAcordo;
import com.skillswap.api.domain.anuncio.Anuncio;
import com.skillswap.api.domain.anuncio.AnuncioRepository;
import com.skillswap.api.domain.anuncio.StatusAnuncio;
import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import com.skillswap.api.domain.mensagem.Mensagem;
import com.skillswap.api.domain.mensagem.MensagemRepository;
import com.skillswap.api.domain.notificacao.NotificacaoService;
import com.skillswap.api.domain.notificacao.TipoNotificacao;
import com.skillswap.api.domain.proposta.Proposta;
import com.skillswap.api.domain.proposta.PropostaRepository;
import com.skillswap.api.domain.proposta.StatusProposta;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioRepository;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConversaService {
    private final ConversaRepository conversas;
    private final PropostaRepository propostas;
    private final AnuncioRepository anuncios;
    private final MensagemRepository mensagens;
    private final UsuarioService usuarios;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoService notificacoes;

    public record Abertura(ConversaDTO conversa, boolean criada) {}
    public record ResultadoAbertura(Conversa conversa, boolean criada) {}

    @Transactional
    public Abertura abrirPorAnuncio(Long anuncioId, String conteudo, String email) {
        var anuncio = anuncioAtivo(anuncioId);
        var destinatarioId = anuncio.getUsuario().getId();
        var resultado = abrir(destinatarioId, anuncio, email);
        if (conteudo != null && !conteudo.isBlank()) enviar(resultado.conversa().getId(), conteudo, anuncioId, email);
        return new Abertura(detalhar(resultado.conversa(), usuarios.findByEmail(email).getId()), resultado.criada());
    }

    @Transactional
    public Abertura abrirPerfil(Long destinatarioId, Long anuncioId, String email) {
        var remetente = usuarios.findByEmail(email);
        var destinatario = usuarios.buscarPerfilPublico(destinatarioId);
        Anuncio anuncio = anuncioId == null ? null : anuncioAtivo(anuncioId);
        if (anuncio != null && !anuncio.getUsuario().getId().equals(destinatarioId))
            throw new IllegalArgumentException("O anúncio informado não pertence ao destinatário");
        var resultado = abrir(destinatario.getId(), anuncio, email);
        return new Abertura(detalhar(resultado.conversa(), remetente.getId()), resultado.criada());
    }

    private ResultadoAbertura abrir(Long destinatarioId, Anuncio anuncio, String email) {
        var remetente = usuarios.findByEmail(email);
        var destinatario = usuarios.buscarPerfilPublico(destinatarioId);
        if (remetente.getId().equals(destinatario.getId()))
            throw new IllegalArgumentException("Não é possível abrir uma conversa consigo mesmo");
        var resultado = obterOuCriar(remetente, destinatario);
        if (anuncio != null) {
            resultado.conversa().setAnuncioContexto(anuncio);
            conversas.save(resultado.conversa());
        }
        return resultado;
    }

    private Anuncio anuncioAtivo(Long anuncioId) {
        return anuncios.findByIdForUpdate(anuncioId)
                .filter(a -> a.getStatus() == StatusAnuncio.ATIVO && a.getUsuario().isPerfilPublico() && !a.isExemplo())
                .orElseThrow(() -> new NaoEncontradoException("Anúncio indisponível"));
    }

    @Transactional
    public ResultadoAbertura obterOuCriar(Usuario primeiro, Usuario segundo) {
        if (primeiro.getId().equals(segundo.getId()))
            throw new IllegalArgumentException("Não é possível abrir uma conversa consigo mesmo");
        long menorId = Math.min(primeiro.getId(), segundo.getId());
        long maiorId = Math.max(primeiro.getId(), segundo.getId());
        var menor = usuarioRepository.findByIdForUpdate(menorId)
                .orElseThrow(() -> new NaoEncontradoException("Participante não encontrado"));
        var maior = usuarioRepository.findByIdForUpdate(maiorId)
                .orElseThrow(() -> new NaoEncontradoException("Participante não encontrado"));
        var existente = conversas.findByParticipanteMenorIdAndParticipanteMaiorId(menorId, maiorId);
        if (existente.isPresent()) return new ResultadoAbertura(existente.get(), false);
        var conversa = conversas.saveAndFlush(Conversa.builder()
                .participanteMenor(menor).participanteMaior(maior).build());
        return new ResultadoAbertura(conversa, true);
    }

    @Transactional(readOnly = true)
    public Page<ConversaDTO> listar(String email, Pageable pageable) {
        var usuario = usuarios.findByEmail(email);
        var pagina = conversas.listarPorParticipante(usuario.getId(), pageable);
        var itens = pagina.getContent();
        var ids = itens.stream().map(Conversa::getId).toList();
        var detalhes = detalhes(itens, usuario.getId());
        return pagina.map(c -> detalhes.get(c.getId()));
    }

    @Transactional(readOnly = true)
    public ConversaDTO buscar(Long id, String email) {
        var usuario = usuarios.findByEmail(email);
        return detalhar(participante(id, usuario, false), usuario.getId());
    }

    @Transactional(readOnly = true)
    public ConversaDTO buscarPorProposta(Long propostaId, String email) {
        var usuario = usuarios.findByEmail(email);
        var proposta = propostas.findById(propostaId)
                .orElseThrow(() -> new NaoEncontradoException("Conversa não encontrada"));
        if (!proposta.getProponente().getId().equals(usuario.getId())
                && !proposta.getAnuncio().getUsuario().getId().equals(usuario.getId()))
            throw new NaoAutorizadoException("Você não participa desta conversa");
        if (proposta.getConversa() == null) throw new NaoEncontradoException("Conversa não encontrada");
        return detalhar(proposta.getConversa(), usuario.getId());
    }

    @Transactional(readOnly = true)
    public long naoLidas(String email) {
        return mensagens.contarNaoLidas(usuarios.findByEmail(email).getId());
    }

    @Transactional(readOnly = true)
    public Page<MensagemConversaDTO> listarMensagens(Long id, String email, Pageable pageable) {
        participante(id, usuarios.findByEmail(email), false);
        return mensagens.findByConversaId(id, pageable).map(MensagemConversaDTO::de);
    }

    @Transactional
    public MensagemConversaDTO enviar(Long id, String conteudo, Long anuncioId, String email) {
        var usuario = usuarios.findByEmail(email);
        var conversa = participante(id, usuario, true);
        Anuncio anuncio = anuncioId == null ? null : anuncioAtivo(anuncioId);
        if (anuncio != null && !participa(conversa, anuncio.getUsuario().getId()))
            throw new NaoAutorizadoException("O anúncio não pertence a um participante desta conversa");
        if (anuncio != null) conversa.setAnuncioContexto(anuncio);
        var mensagem = mensagens.saveAndFlush(Mensagem.builder().conversa(conversa).anuncioContexto(anuncio)
                .remetente(usuario).conteudo(conteudo.strip()).build());
        conversa.setUltimaAtividadeEm(mensagem.getEnviadoEm());
        var destinatario = outro(conversa, usuario.getId());
        var contexto = anuncio == null ? "a conversa" : "“" + anuncio.getTitulo() + "”";
        notificacoes.criar(destinatario, TipoNotificacao.NOVA_MENSAGEM, "Nova mensagem",
                usuario.getNome() + " enviou uma mensagem sobre " + contexto + ".",
                "/mensagens?conversa=chat-" + id);
        return MensagemConversaDTO.de(mensagem);
    }

    @Transactional
    public void ler(Long id, Long ultimaMensagemId, String email) {
        var usuario = usuarios.findByEmail(email);
        var conversa = participante(id, usuario, true);
        var mensagem = mensagens.findByIdAndConversaId(ultimaMensagemId, id)
                .orElseThrow(() -> new NaoEncontradoException("Mensagem não encontrada"));
        atualizarCursorConversa(conversa, usuario.getId(), mensagem);
        for (var ultima : mensagens.ultimasPorPropostaAte(id, mensagem.getEnviadoEm(), mensagem.getId()))
            atualizarCursorProposta(ultima.getProposta(), usuario.getId(), ultima);
    }

    @Transactional(readOnly = true)
    public MensagemConversaDTO buscarMensagem(Long conversaId, Long mensagemId, String email) {
        participante(conversaId, usuarios.findByEmail(email), false);
        return mensagens.findByIdAndConversaId(mensagemId, conversaId).map(MensagemConversaDTO::de)
                .orElseThrow(() -> new NaoEncontradoException("Mensagem não encontrada"));
    }

    private Conversa participante(Long id, Usuario usuario, boolean bloquear) {
        var conversa = (bloquear ? conversas.findByIdForUpdate(id) : conversas.findById(id))
                .orElseThrow(() -> new NaoEncontradoException("Conversa não encontrada"));
        if (!participa(conversa, usuario.getId())) throw new NaoAutorizadoException("Você não participa desta conversa");
        return conversa;
    }

    @Transactional(readOnly = true)
    public Conversa exigirParticipante(Long id, String email) {
        return participante(id, usuarios.findByEmail(email), false);
    }

    @Transactional(readOnly = true)
    public Usuario outroParticipante(Conversa conversa, String email) {
        var usuario = usuarios.findByEmail(email);
        if (!participa(conversa, usuario.getId()))
            throw new NaoAutorizadoException("Você não participa desta conversa");
        return outro(conversa, usuario.getId());
    }

    private boolean participa(Conversa conversa, Long id) {
        return conversa.getParticipanteMenor().getId().equals(id) || conversa.getParticipanteMaior().getId().equals(id);
    }

    private Usuario outro(Conversa conversa, Long id) {
        return conversa.getParticipanteMenor().getId().equals(id)
                ? conversa.getParticipanteMaior() : conversa.getParticipanteMenor();
    }

    private void atualizarCursorConversa(Conversa conversa, Long usuarioId, Mensagem mensagem) {
        boolean menor = conversa.getParticipanteMenor().getId().equals(usuarioId);
        var dataAtual = menor ? conversa.getUltimaLeituraMenorEm() : conversa.getUltimaLeituraMaiorEm();
        long idAtual = menor ? conversa.getUltimaLidaMenor() : conversa.getUltimaLidaMaior();
        if (dataAtual == null || mensagem.getEnviadoEm().isAfter(dataAtual)
                || (mensagem.getEnviadoEm().equals(dataAtual) && mensagem.getId() > idAtual)) {
            if (menor) {
                conversa.setUltimaLidaMenor(mensagem.getId());
                conversa.setUltimaLeituraMenorEm(mensagem.getEnviadoEm());
            } else {
                conversa.setUltimaLidaMaior(mensagem.getId());
                conversa.setUltimaLeituraMaiorEm(mensagem.getEnviadoEm());
            }
        }
    }

    private void atualizarCursorProposta(Proposta proposta, Long usuarioId, Mensagem mensagem) {
        boolean proponente = proposta.getProponente().getId().equals(usuarioId);
        var dataAtual = proponente ? proposta.getUltimaLeituraProponenteEm() : proposta.getUltimaLeituraAnuncianteEm();
        long idAtual = proponente ? proposta.getUltimaLidaProponente() : proposta.getUltimaLidaAnunciante();
        if (dataAtual == null || mensagem.getEnviadoEm().isAfter(dataAtual)
                || (mensagem.getEnviadoEm().equals(dataAtual) && mensagem.getId() > idAtual)) {
            if (proponente) {
                proposta.setUltimaLidaProponente(mensagem.getId());
                proposta.setUltimaLeituraProponenteEm(mensagem.getEnviadoEm());
            } else {
                proposta.setUltimaLidaAnunciante(mensagem.getId());
                proposta.setUltimaLeituraAnuncianteEm(mensagem.getEnviadoEm());
            }
        }
    }

    private ConversaDTO detalhar(Conversa conversa, Long usuarioId) {
        return detalhes(List.of(conversa), usuarioId).get(conversa.getId());
    }

    private Map<Long, ConversaDTO> detalhes(List<Conversa> itens, Long usuarioId) {
        if (itens.isEmpty()) return Map.of();
        var ids = itens.stream().map(Conversa::getId).toList();
        var ultimas = new HashMap<Long, Mensagem>();
        mensagens.ultimas(ids).forEach(m -> ultimas.put(m.getConversa().getId(), m));
        var naoLidas = new HashMap<Long, Long>();
        mensagens.contarPorConversa(ids, usuarioId).forEach(c -> naoLidas.put(c.getConversaId(), c.getQuantidade()));
        var resultado = new HashMap<Long, ConversaDTO>();
        for (var conversa : itens) {
            var ultima = ultimas.get(conversa.getId());
            var outro = outro(conversa, usuarioId);
            Anuncio anuncio = ultima == null ? conversa.getAnuncioContexto() : anuncioDe(ultima);
            var proposta = ultima == null ? null : ultima.getProposta();
            var acordo = ultima == null ? null : ultima.getAcordo();
            boolean imagemPublica = anuncio != null && anuncio.getStatus() == StatusAnuncio.ATIVO
                    && anuncio.getUsuario().isPerfilPublico();
            resultado.put(conversa.getId(), new ConversaDTO(conversa.getId(), outro.getId(), outro.getNome(),
                    outro.isPerfilPublico() ? outro.getFotoUrl() : null,
                    anuncio == null ? null : anuncio.getId(), anuncio == null ? null : anuncio.getTitulo(),
                    imagemPublica ? anuncio.getImagem() : null,
                    anuncio == null ? null : anuncio.getCidade(), anuncio == null ? null : anuncio.getModalidade().name(),
                    proposta == null ? null : proposta.getStatus(), acordo == null ? null : acordo.getId(),
                    acordo == null ? null : acordo.getStatus(), true,
                    ultima == null ? null : MensagemConversaDTO.de(ultima),
                    naoLidas.getOrDefault(conversa.getId(), 0L), conversa.getCriadoEm(), conversa.getUltimaAtividadeEm()));
        }
        return resultado;
    }

    private Anuncio anuncioDe(Mensagem mensagem) {
        if (mensagem.getAnuncioContexto() != null) return mensagem.getAnuncioContexto();
        if (mensagem.getProposta() != null) return mensagem.getProposta().getAnuncio();
        return mensagem.getAcordo() == null ? null : mensagem.getAcordo().getProposta().getAnuncio();
    }
}
