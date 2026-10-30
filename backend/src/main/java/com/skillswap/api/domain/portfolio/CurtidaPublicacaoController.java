package com.skillswap.api.domain.portfolio;

import com.skillswap.api.domain.portfolio.dto.CurtidaPublicacaoDTO;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publicacoes/{id}/curtida")
@RequiredArgsConstructor
@Validated
public class CurtidaPublicacaoController {
    private final PublicacaoPortfolioService service;

    @PutMapping
    public ResponseEntity<CurtidaPublicacaoDTO> curtir(@PathVariable @Positive Long id, Authentication auth) {
        return ResponseEntity.ok(service.alterarCurtida(id, (String) auth.getPrincipal(), true));
    }

    @DeleteMapping
    public ResponseEntity<CurtidaPublicacaoDTO> deixarDeCurtir(@PathVariable @Positive Long id, Authentication auth) {
        return ResponseEntity.ok(service.alterarCurtida(id, (String) auth.getPrincipal(), false));
    }
}
