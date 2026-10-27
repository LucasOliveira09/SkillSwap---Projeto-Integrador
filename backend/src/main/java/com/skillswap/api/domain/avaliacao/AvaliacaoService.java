package com.skillswap.api.domain.avaliacao;

import com.skillswap.api.domain.acordo.Acordo;
import com.skillswap.api.domain.acordo.AcordoService;
import com.skillswap.api.domain.acordo.StatusAcordo;
import com.skillswap.api.domain.avaliacao.dto.AvaliacaoRequestDTO;
import com.skillswap.api.domain.avaliacao.dto.AvaliacaoResponseDTO;
import com.skillswap.api.domain.avaliacao.dto.RespostaAvaliacaoDTO;
import com.skillswap.api.domain.avaliacao.dto.AvaliacaoHistoricoDTO;
import com.skillswap.api.domain.avaliacao.dto.EditarAvaliacaoDTO;
import com.skillswap.api.domain.moderacao.DenunciaRepository;
import com.skillswap.api.domain.moderacao.TipoConteudoDenunciado;
import com.skillswap.api.domain.avaliacao.ContestacaoAvaliacaoRepository;
import com.skillswap.api.domain.moderacao.ModeracaoService;
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
    private final AvaliacaoHistoricoRepository historicoRepository;
    private final DenunciaRepository denunciaRepository;
    private final ContestacaoAvaliacaoRepository contestacaoRepository;
    private final ModeracaoService moderacaoService;

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

    @Transactional
    public AvaliacaoResponseDTO editar(Long acordoId, Long avaliacaoId, EditarAvaliacaoDTO dto, String emailUsuario) {
        Acordo acordo = acordoService.findById(acordoId);
        String emailAnunciante = acordo.getProposta().getAnuncio().getUsuario().getEmail();
        String emailProponente = acordo.getProposta().getProponente().getEmail();
        if (!emailAnunciante.equalsIgnoreCase(emailUsuario) && !emailProponente.equalsIgnoreCase(emailUsuario)) {
            throw new NaoAutorizadoException("Você não participa deste acordo.");
        }
        if (acordo.getStatus() != StatusAcordo.CONCLUIDO) {
            throw new ConflitoException("A avaliação só pode ser editada após a conclusão do acordo.");
        }

        Avaliacao avaliacao = avaliacaoRepository.findByIdAndAcordoIdForUpdate(avaliacaoId, acordoId)
                .orElseThrow(() -> new NaoEncontradoException("Avaliação não encontrada"));
        Usuario editor = usuarioService.findByEmail(emailUsuario);
        if (!avaliacao.getAvaliador().getId().equals(editor.getId())
                || !editor.getEmail().equalsIgnoreCase(emailProponente)) {
            throw new NaoAutorizadoException("Somente o cliente que escreveu a avaliação pode editá-la.");
        }
        if (avaliacao.isEdicaoRealizada()) {
            String depoimento = dto.depoimento() == null ? null : dto.depoimento().strip();
            if (avaliacao.getNota() == dto.nota()
                    && java.util.Objects.equals(avaliacao.getNotaAnuncio(), dto.notaAnuncio())
                    && java.util.Objects.equals(avaliacao.getDepoimento(), depoimento)) {
                return toResponseDTO(avaliacao);
            }
            throw new ConflitoException("Esta avaliação já foi editada.");
        }
        if (avaliacao.getCriadoEm().plusHours(24).isBefore(LocalDateTime.now())) {
            throw new ConflitoException("O prazo de 24 horas para editar esta avaliação terminou.");
        }
        if (avaliacao.getRespostaProfissional() != null) {
            throw new ConflitoException("Uma avaliação respondida pelo profissional não pode ser editada.");
        }
        if (denunciaRepository.existsByTipoConteudoAndRecursoId(TipoConteudoDenunciado.AVALIACAO, avaliacaoId)
                || contestacaoRepository.existsByAvaliacaoId(avaliacaoId)) {
            throw new ConflitoException("Uma avaliação denunciada ou contestada não pode ser editada.");
        }
        if (avaliacao.isModeradaOculta()) {
            throw new ConflitoException("Uma avaliação ocultada não pode ser editada.");
        }

        var agora = LocalDateTime.now();
        historicoRepository.saveAndFlush(AvaliacaoHistorico.builder()
                .avaliacao(avaliacao)
                .editor(editor)
                .notaAnterior(avaliacao.getNota())
                .notaAnuncioAnterior(avaliacao.getNotaAnuncio())
                .depoimentoAnterior(avaliacao.getDepoimento())
                .registradaEm(agora)
                .build());
        avaliacao.setNota(dto.nota());
        avaliacao.setNotaAnuncio(dto.notaAnuncio());
        avaliacao.setDepoimento(dto.depoimento() == null ? null : dto.depoimento().strip());
        avaliacao.setEdicaoRealizada(true);
        avaliacao.setEditadaEm(agora);
        return toResponseDTO(avaliacaoRepository.save(avaliacao));
    }

    @Transactional(readOnly = true)
    public AvaliacaoHistoricoDTO historicoParaModeracao(Long avaliacaoId, String email) {
        if (!moderacaoService.podeModerar(email)) {
            throw new NaoAutorizadoException("Acesso restrito à equipe de moderação.");
        }
        var historico = historicoRepository.findByAvaliacaoId(avaliacaoId)
                .orElseThrow(() -> new NaoEncontradoException("Histórico de edição não encontrado"));
        return new AvaliacaoHistoricoDTO(avaliacaoId, historico.getNotaAnterior(),
                historico.getNotaAnuncioAnterior(), historico.getDepoimentoAnterior(),
                historico.getEditor().getNome(), historico.getRegistradaEm());
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
                a.getRespondidaEm(),
                a.isEdicaoRealizada(),
                a.getEditadaEm()
        );
    }
}
