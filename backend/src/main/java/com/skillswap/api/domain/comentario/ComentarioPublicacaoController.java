package com.skillswap.api.domain.comentario;

import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/publicacoes/{publicacaoId}/comentarios")
@RequiredArgsConstructor
@Validated
public class ComentarioPublicacaoController {
    private final ComentarioPublicacaoService service;

    @GetMapping
    public ResponseEntity<PaginaDTO<ComentarioPublicacaoDTO>> listar(
            @PathVariable @Positive Long publicacaoId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "30") @Min(1) @Max(50) int tamanho,
            Authentication authentication) {
        String email = authentication == null || authentication instanceof AnonymousAuthenticationToken
                ? null : (String) authentication.getPrincipal();
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm", "id"));
        return ResponseEntity.ok(PaginaDTO.de(service.listar(publicacaoId, email, pageable)));
    }

    @PostMapping
    public ResponseEntity<ComentarioPublicacaoDTO> criar(
            @PathVariable @Positive Long publicacaoId,
            @RequestBody @Valid ComentarioPublicacaoRequestDTO dto,
            Authentication authentication) {
        var comentario = service.criar(publicacaoId, dto, (String) authentication.getPrincipal());
        return ResponseEntity.created(URI.create("/api/publicacoes/" + publicacaoId + "/comentarios/" + comentario.id()))
                .body(comentario);
    }
}
