package com.skillswap.api.domain.anuncio;

import com.skillswap.api.domain.anuncio.dto.AnuncioRequestDTO;
import com.skillswap.api.domain.anuncio.dto.AnuncioResponseDTO;
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
    public ResponseEntity<Page<AnuncioResponseDTO>> listar(
            @RequestParam(required = false) CategoriaAnuncio categoria,
            @RequestParam(required = false) String cidade,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(anuncioService.listar(categoria, cidade, pageable));
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
        return ResponseEntity.status(HttpStatus.CREATED).body(anuncioService.toResponseDTO(anuncio));
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
