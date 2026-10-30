package com.skillswap.api.domain.avaliacao;

import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import com.skillswap.api.domain.avaliacao.dto.ContestacaoAvaliacaoDTO;
import com.skillswap.api.domain.avaliacao.dto.CriarContestacaoAvaliacaoDTO;
import com.skillswap.api.domain.avaliacao.dto.DecidirContestacaoAvaliacaoDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ContestacaoAvaliacaoController {
    private final ContestacaoAvaliacaoService contestacaoService;

    @PostMapping("/api/avaliacoes/{avaliacaoId}/contestacoes")
    public ResponseEntity<ContestacaoAvaliacaoDTO> criar(
            @PathVariable Long avaliacaoId,
            @RequestBody @Valid CriarContestacaoAvaliacaoDTO dto,
            Authentication authentication) {
        var contestacao = contestacaoService.criar(avaliacaoId, dto, (String) authentication.getPrincipal());
        return ResponseEntity.created(URI.create(
                "/api/avaliacoes/" + avaliacaoId + "/contestacoes/" + contestacao.id()))
                .body(contestacao);
    }

    @GetMapping("/api/avaliacoes/{avaliacaoId}/contestacoes")
    public ResponseEntity<List<ContestacaoAvaliacaoDTO>> listarDoProfissional(
            @PathVariable Long avaliacaoId, Authentication authentication) {
        var contestacoes = contestacaoService.listarDoProfissional(
                avaliacaoId, (String) authentication.getPrincipal());
        return ResponseEntity.ok(contestacoes);
    }

    @GetMapping("/api/avaliacoes/{avaliacaoId}/contestacoes/{contestacaoId}")
    public ResponseEntity<ContestacaoAvaliacaoDTO> buscarDoProfissional(
            @PathVariable Long avaliacaoId, @PathVariable Long contestacaoId, Authentication authentication) {
        return ResponseEntity.ok(contestacaoService.buscarDoProfissional(
                avaliacaoId, contestacaoId, (String) authentication.getPrincipal()));
    }

    @GetMapping("/api/moderacao/contestacoes")
    public ResponseEntity<PaginaDTO<ContestacaoAvaliacaoDTO>> listar(
            @RequestParam(defaultValue = "ABERTA") StatusContestacaoAvaliacao status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int tamanho,
            Authentication authentication) {
        return ResponseEntity.ok(contestacaoService.listar(
                status, pagina, tamanho, (String) authentication.getPrincipal()));
    }

    @PatchMapping("/api/moderacao/contestacoes/{id}")
    public ResponseEntity<ContestacaoAvaliacaoDTO> decidir(
            @PathVariable Long id,
            @RequestBody @Valid DecidirContestacaoAvaliacaoDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(contestacaoService.decidir(id, dto, (String) authentication.getPrincipal()));
    }
}
