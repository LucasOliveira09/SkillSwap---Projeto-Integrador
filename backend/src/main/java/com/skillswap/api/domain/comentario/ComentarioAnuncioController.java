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
@RequestMapping("/api/anuncios/{anuncioId}/comentarios")
@RequiredArgsConstructor
@Validated
public class ComentarioAnuncioController {
    private final ComentarioAnuncioService service;

    @GetMapping
    public ResponseEntity<PaginaDTO<ComentarioAnuncioDTO>> listar(
            @PathVariable @Positive Long anuncioId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "30") @Min(1) @Max(50) int tamanho,
            Authentication authentication) {
        String email = authentication == null || authentication instanceof AnonymousAuthenticationToken
                ? null : (String) authentication.getPrincipal();
        var resultado = service.listar(anuncioId, email,
                PageRequest.of(pagina, tamanho, Sort.by("criadoEm", "id")));
        return ResponseEntity.ok(PaginaDTO.de(resultado));
    }

    @PostMapping
    public ResponseEntity<ComentarioAnuncioDTO> criar(
            @PathVariable @Positive Long anuncioId,
            @RequestBody @Valid ComentarioAnuncioRequestDTO dto,
            Authentication authentication) {
        String email = authentication == null || authentication instanceof AnonymousAuthenticationToken
                ? null : (String) authentication.getPrincipal();
        var comentario = service.criar(anuncioId, dto, email);
        return ResponseEntity.created(URI.create("/api/anuncios/" + anuncioId + "/comentarios/" + comentario.id()))
                .body(comentario);
    }
}
