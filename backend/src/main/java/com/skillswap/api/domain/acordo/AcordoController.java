package com.skillswap.api.domain.acordo;

import com.skillswap.api.domain.acordo.dto.AcordoResponseDTO;
import com.skillswap.api.domain.acordo.dto.AtualizarStatusAcordoDTO;
import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/acordos")
@RequiredArgsConstructor
public class AcordoController {

    private final AcordoService acordoService;

    @GetMapping
    public ResponseEntity<PaginaDTO<AcordoResponseDTO>> listar(Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int tamanho) {
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm", "id"));
        return ResponseEntity.ok(PaginaDTO.de(acordoService.listarMeus(
                (String) authentication.getPrincipal(), pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcordoResponseDTO> buscar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(acordoService.buscar(id, (String) authentication.getPrincipal()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AcordoResponseDTO> atualizarStatus(@PathVariable Long id,
            @RequestBody @Valid AtualizarStatusAcordoDTO dto, Authentication authentication) {
        var acordo = acordoService.atualizarStatus(id, dto, (String) authentication.getPrincipal());
        return ResponseEntity.ok(acordoService.toResponseDTO(acordo));
    }

    @GetMapping("/meus")
    public ResponseEntity<List<AcordoResponseDTO>> listarMeus(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(acordoService.listarMeus(email));
    }

    @PatchMapping("/{id}/concluir")
    public ResponseEntity<AcordoResponseDTO> concluir(
            @PathVariable Long id,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var acordo = acordoService.concluir(id, email);
        return ResponseEntity.ok(acordoService.toResponseDTO(acordo));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<AcordoResponseDTO> cancelar(
            @PathVariable Long id,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var acordo = acordoService.cancelar(id, email);
        return ResponseEntity.ok(acordoService.toResponseDTO(acordo));
    }
}
