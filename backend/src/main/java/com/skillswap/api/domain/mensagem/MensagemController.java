package com.skillswap.api.domain.mensagem;

import com.skillswap.api.domain.mensagem.dto.MensagemRequestDTO;
import com.skillswap.api.domain.mensagem.dto.MensagemResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/acordos/{acordoId}/mensagens")
@RequiredArgsConstructor
public class MensagemController {

    private final MensagemService mensagemService;

    @GetMapping
    public ResponseEntity<List<MensagemResponseDTO>> listar(
            @PathVariable Long acordoId,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(mensagemService.listarPorAcordo(acordoId, email));
    }

    @PostMapping
    public ResponseEntity<MensagemResponseDTO> enviar(
            @PathVariable Long acordoId,
            @RequestBody @Valid MensagemRequestDTO dto,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var mensagem = mensagemService.enviar(acordoId, dto, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(mensagemService.toResponseDTO(mensagem));
    }
}
