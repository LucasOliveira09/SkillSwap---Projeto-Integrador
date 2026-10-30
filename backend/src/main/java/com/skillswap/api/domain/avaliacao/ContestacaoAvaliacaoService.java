package com.skillswap.api.domain.avaliacao;

import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import com.skillswap.api.domain.avaliacao.dto.ContestacaoAvaliacaoDTO;
import com.skillswap.api.domain.avaliacao.dto.CriarContestacaoAvaliacaoDTO;
import com.skillswap.api.domain.avaliacao.dto.DecidirContestacaoAvaliacaoDTO;
import com.skillswap.api.domain.moderacao.ModeracaoService;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.infra.exception.ConflitoException;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContestacaoAvaliacaoService {
    private static final int PRAZO_CONTESTACAO_DIAS = 7;

    private final ContestacaoAvaliacaoRepository contestacaoRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioService usuarioService;
    private final ModeracaoService moderacaoService;

    @Transactional
    public ContestacaoAvaliacaoDTO criar(Long avaliacaoId, CriarContestacaoAvaliacaoDTO dto, String email) {
        var profissional = usuarioService.findByEmail(email);
        var avaliacao = avaliacaoRepository.findByIdForUpdate(avaliacaoId)
                .orElseThrow(() -> new NaoEncontradoException("Avaliação não encontrada"));

        if (!avaliacao.getAvaliado().getId().equals(profissional.getId())) {
            throw new NaoAutorizadoException("Somente o profissional avaliado pode contestar esta avaliação.");
        }
        if (avaliacao.isModeradaOculta()) {
            throw new ConflitoException("Uma avaliação já ocultada não pode receber contestação.");
        }
        if (avaliacao.getCriadoEm().plusDays(PRAZO_CONTESTACAO_DIAS).isBefore(LocalDateTime.now())) {
            throw new ConflitoException("O prazo de sete dias para contestar esta avaliação terminou.");
        }
        if (contestacaoRepository.existsByAvaliacaoId(avaliacaoId)) {
            throw new ConflitoException("Esta avaliação já possui uma contestação registrada.");
        }

        var contestacao = ContestacaoAvaliacao.builder()
                .avaliacao(avaliacao)
                .contestador(profissional)
                .justificativa(dto.justificativa().strip())
                .build();
        try {
            return paraDTO(contestacaoRepository.saveAndFlush(contestacao));
        } catch (DataIntegrityViolationException duplicada) {
            if (contestacaoRepository.existsByAvaliacaoId(avaliacaoId)) {
                throw new ConflitoException("Esta avaliação já possui uma contestação registrada.");
            }
            throw duplicada;
        }
    }

    @Transactional(readOnly = true)
    public List<ContestacaoAvaliacaoDTO> listarDoProfissional(Long avaliacaoId, String email) {
        var profissional = usuarioService.findByEmail(email);
        var avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new NaoEncontradoException("Avaliação não encontrada"));
        if (!avaliacao.getAvaliado().getId().equals(profissional.getId())) {
            throw new NaoAutorizadoException("Somente o profissional avaliado pode consultar esta contestação.");
        }
        return contestacaoRepository.findByAvaliacaoId(avaliacaoId).stream().map(this::paraDTO).toList();
    }

    @Transactional(readOnly = true)
    public ContestacaoAvaliacaoDTO buscarDoProfissional(Long avaliacaoId, Long contestacaoId, String email) {
        var profissional = usuarioService.findByEmail(email);
        var contestacao = contestacaoRepository.findByIdAndAvaliacaoId(contestacaoId, avaliacaoId)
                .orElseThrow(() -> new NaoEncontradoException("Contestação não encontrada"));
        if (!contestacao.getContestador().getId().equals(profissional.getId())) {
            throw new NaoAutorizadoException("Somente o profissional avaliado pode consultar esta contestação.");
        }
        return paraDTO(contestacao);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ContestacaoAvaliacaoDTO> listar(
            StatusContestacaoAvaliacao status, int pagina, int tamanho, String email) {
        exigirModerador(email);
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.ASC, "criadaEm", "id"));
        return PaginaDTO.de(contestacaoRepository.findByStatusOrderByCriadaEmAscIdAsc(status, pageable)
                .map(this::paraDTO));
    }

    @Transactional
    public ContestacaoAvaliacaoDTO decidir(Long id, DecidirContestacaoAvaliacaoDTO dto, String email) {
        exigirModerador(email);
        var contestacao = contestacaoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NaoEncontradoException("Contestação não encontrada"));
        if (contestacao.getStatus() != StatusContestacaoAvaliacao.ABERTA) {
            throw new ConflitoException("Esta contestação já foi analisada.");
        }

        var avaliacao = avaliacaoRepository.findByIdForUpdate(contestacao.getAvaliacao().getId())
                .orElseThrow(() -> new NaoEncontradoException("Avaliação não encontrada"));
        if (dto.acao() == AcaoContestacaoAvaliacao.OCULTAR) {
            avaliacao.setModeradaOculta(true);
            contestacao.setStatus(StatusContestacaoAvaliacao.OCULTADA);
        } else {
            contestacao.setStatus(StatusContestacaoAvaliacao.MANTIDA);
        }
        contestacao.setDecisao(dto.decisao().strip());
        contestacao.setAnalisadaEm(LocalDateTime.now());
        contestacao.setAnalisadaPor(email);
        avaliacaoRepository.save(avaliacao);
        return paraDTO(contestacaoRepository.save(contestacao));
    }

    private void exigirModerador(String email) {
        if (!moderacaoService.podeModerar(email)) {
            throw new NaoAutorizadoException("Acesso restrito à equipe de moderação.");
        }
    }

    private ContestacaoAvaliacaoDTO paraDTO(ContestacaoAvaliacao contestacao) {
        var avaliacao = contestacao.getAvaliacao();
        return new ContestacaoAvaliacaoDTO(
                contestacao.getId(), avaliacao.getId(), avaliacao.getAcordo().getId(),
                contestacao.getContestador().getId(), contestacao.getContestador().getNome(),
                avaliacao.getAvaliador().getNome(), avaliacao.getAvaliado().getNome(),
                avaliacao.getNota(), avaliacao.getNotaAnuncio(), avaliacao.getDepoimento(),
                contestacao.getJustificativa(), contestacao.getStatus(), contestacao.getCriadaEm(),
                contestacao.getDecisao(), contestacao.getAnalisadaEm(), contestacao.getAnalisadaPor());
    }
}
