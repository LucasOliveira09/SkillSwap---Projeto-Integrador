package com.skillswap.api.domain.acordo;

import com.skillswap.api.domain.acordo.dto.AcordoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/acordos")
@RequiredArgsConstructor
public class AcordoController {

    private final AcordoService acordoService;

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
