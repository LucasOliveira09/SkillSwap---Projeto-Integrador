package com.skillswap.api.domain.avaliacao;

import com.skillswap.api.domain.avaliacao.dto.AvaliacaoRequestDTO;
import com.skillswap.api.domain.avaliacao.dto.AvaliacaoResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @PostMapping("/api/acordos/{acordoId}/avaliacoes")
    public ResponseEntity<AvaliacaoResponseDTO> criar(
            @PathVariable Long acordoId,
            @RequestBody @Valid AvaliacaoRequestDTO dto,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var avaliacao = avaliacaoService.criar(acordoId, dto, email);
        return ResponseEntity.created(java.net.URI.create("/api/acordos/" + acordoId + "/avaliacoes/" + avaliacao.getId()))
                .body(avaliacaoService.toResponseDTO(avaliacao));
    }

    @GetMapping("/api/acordos/{acordoId}/avaliacoes")
    public ResponseEntity<List<AvaliacaoResponseDTO>> listarPorAcordo(
            @PathVariable Long acordoId, Authentication authentication) {
        return ResponseEntity.ok(avaliacaoService.listarPorAcordo(acordoId, (String) authentication.getPrincipal()));
    }

    @GetMapping("/api/acordos/{acordoId}/avaliacoes/{avaliacaoId}")
    public ResponseEntity<AvaliacaoResponseDTO> buscarPorAcordo(@PathVariable Long acordoId,
            @PathVariable Long avaliacaoId, Authentication authentication) {
        return ResponseEntity.ok(avaliacaoService.buscarPorAcordo(
                acordoId, avaliacaoId, (String) authentication.getPrincipal()));
    }

    @GetMapping("/api/usuarios/{usuarioId}/avaliacoes")
    public ResponseEntity<List<AvaliacaoResponseDTO>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(avaliacaoService.listarPorUsuario(usuarioId));
    }
}
