package br.edu.unifio.skillswap.controllers;

import br.edu.unifio.skillswap.dto.*;
import br.edu.unifio.skillswap.services.AnuncioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/anuncios")
@RequiredArgsConstructor
public class AnuncioController {
    private final AnuncioService service;

    @GetMapping
    public PaginaResponse<AnuncioResponse> listar(
        @RequestParam(defaultValue = "") @Size(max = 100) String busca,
        @RequestParam(defaultValue = "") @Size(max = 40) String categoria,
        @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
        @RequestParam(defaultValue = "5") @Min(1) @Max(50) int tamanho
    ) {
        return service.listar(busca, categoria, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public AnuncioResponse buscar(@PathVariable Integer id) { return service.buscar(id); }

    @PostMapping
    public ResponseEntity<AnuncioResponse> criar(@RequestBody @Valid AnuncioRequest dados) {
        var anuncio = service.criar(dados);
        return ResponseEntity.created(URI.create("/api/anuncios/" + anuncio.id())).body(anuncio);
    }
}
