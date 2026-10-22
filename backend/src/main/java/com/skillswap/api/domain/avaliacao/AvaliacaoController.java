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
        return ResponseEntity.status(HttpStatus.CREATED).body(avaliacaoService.toResponseDTO(avaliacao));
    }

    @GetMapping("/api/usuarios/{usuarioId}/avaliacoes")
    public ResponseEntity<List<AvaliacaoResponseDTO>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(avaliacaoService.listarPorUsuario(usuarioId));
    }
}
