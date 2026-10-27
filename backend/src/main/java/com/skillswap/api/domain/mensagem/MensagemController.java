package com.skillswap.api.domain.mensagem;

import com.skillswap.api.domain.mensagem.dto.MensagemRequestDTO;
import com.skillswap.api.domain.mensagem.dto.MensagemResponseDTO;
import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/acordos/{acordoId}/mensagens")
@RequiredArgsConstructor
public class MensagemController {

    private final MensagemService mensagemService;

    @GetMapping
    public ResponseEntity<PaginaDTO<MensagemResponseDTO>> listar(
            @PathVariable Long acordoId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int tamanho,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.ASC, "enviadoEm", "id"));
        return ResponseEntity.ok(PaginaDTO.de(mensagemService.listarPorAcordo(acordoId, email, pageable)));
    }

    @GetMapping("/{mensagemId}")
    public ResponseEntity<MensagemResponseDTO> buscar(@PathVariable Long acordoId,
            @PathVariable Long mensagemId, Authentication authentication) {
        return ResponseEntity.ok(mensagemService.buscar(acordoId, mensagemId, (String) authentication.getPrincipal()));
    }

    @PostMapping
    public ResponseEntity<MensagemResponseDTO> enviar(
            @PathVariable Long acordoId,
            @RequestBody @Valid MensagemRequestDTO dto,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var mensagem = mensagemService.enviar(acordoId, dto, email);
        return ResponseEntity.created(java.net.URI.create("/api/acordos/" + acordoId + "/mensagens/" + mensagem.getId()))
                .body(mensagemService.toResponseDTO(mensagem));
    }
}
