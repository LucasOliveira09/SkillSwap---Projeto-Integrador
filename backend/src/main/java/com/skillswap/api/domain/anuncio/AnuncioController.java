package com.skillswap.api.domain.anuncio;

import com.skillswap.api.domain.anuncio.dto.AnuncioRequestDTO;
import com.skillswap.api.domain.anuncio.dto.AnuncioResponseDTO;
import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/anuncios")
@RequiredArgsConstructor
public class AnuncioController {

    private final AnuncioService anuncioService;

    @GetMapping
    public ResponseEntity<PaginaDTO<AnuncioResponseDTO>> listar(
            @RequestParam(defaultValue = "") @Size(max = 40) String categoria,
            @RequestParam(required = false) String cidade,
            @RequestParam(defaultValue = "") @Size(max = 100) String busca,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int tamanho) {
        var pageable = PageRequest.of(pagina,tamanho, Sort.by(Sort.Direction.DESC,"criadoEm","id"));
        return ResponseEntity.ok(PaginaDTO.de(anuncioService.pesquisar(categoria,cidade,busca,pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnuncioResponseDTO> buscar(@PathVariable Long id) {
        var anuncio = anuncioService.buscarPorId(id);
        return ResponseEntity.ok(anuncioService.toResponseDTO(anuncio));
    }

    @PostMapping
    public ResponseEntity<AnuncioResponseDTO> criar(
            @RequestBody @Valid AnuncioRequestDTO dto,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var anuncio = anuncioService.criar(dto, email);
        return ResponseEntity.created(java.net.URI.create("/api/anuncios/"+anuncio.getId()))
            .body(anuncioService.toResponseDTO(anuncio));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnuncioResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid AnuncioRequestDTO dto,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var anuncio = anuncioService.atualizar(id, dto, email);
        return ResponseEntity.ok(anuncioService.toResponseDTO(anuncio));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Long id, Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        anuncioService.inativar(id, email);
        return ResponseEntity.noContent().build();
    }
}
