package com.skillswap.api.domain.proposta;

import com.skillswap.api.domain.proposta.dto.PropostaRequestDTO;
import com.skillswap.api.domain.proposta.dto.PropostaResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PropostaController {

    private final PropostaService propostaService;

    @PostMapping("/anuncios/{anuncioId}/propostas")
    public ResponseEntity<PropostaResponseDTO> criar(
            @PathVariable Long anuncioId,
            @RequestBody @Valid PropostaRequestDTO dto,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var proposta = propostaService.criar(anuncioId, dto, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(propostaService.toResponseDTO(proposta));
    }

    @GetMapping("/propostas/minhas")
    public ResponseEntity<List<PropostaResponseDTO>> listarMinhas(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(propostaService.listarEnviadas(email));
    }

    @GetMapping("/propostas/recebidas")
    public ResponseEntity<List<PropostaResponseDTO>> listarRecebidas(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(propostaService.listarRecebidas(email));
    }

    @PatchMapping("/propostas/{id}/aceitar")
    public ResponseEntity<PropostaResponseDTO> aceitar(
            @PathVariable Long id,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var proposta = propostaService.aceitar(id, email);
        return ResponseEntity.ok(propostaService.toResponseDTO(proposta));
    }

    @PatchMapping("/propostas/{id}/recusar")
    public ResponseEntity<PropostaResponseDTO> recusar(
            @PathVariable Long id,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var proposta = propostaService.recusar(id, email);
        return ResponseEntity.ok(propostaService.toResponseDTO(proposta));
    }
}
