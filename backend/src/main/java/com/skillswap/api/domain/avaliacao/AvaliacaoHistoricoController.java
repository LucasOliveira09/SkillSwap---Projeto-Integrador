package com.skillswap.api.domain.avaliacao;

import com.skillswap.api.domain.avaliacao.dto.AvaliacaoHistoricoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AvaliacaoHistoricoController {
    private final AvaliacaoService avaliacaoService;

    @GetMapping("/api/moderacao/avaliacoes/{avaliacaoId}/historico")
    public ResponseEntity<AvaliacaoHistoricoDTO> historico(
            @PathVariable Long avaliacaoId, Authentication authentication) {
        return ResponseEntity.ok(avaliacaoService.historicoParaModeracao(
                avaliacaoId, (String) authentication.getPrincipal()));
    }
}
