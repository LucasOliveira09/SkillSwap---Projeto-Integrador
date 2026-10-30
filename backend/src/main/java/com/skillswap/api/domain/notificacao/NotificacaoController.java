package com.skillswap.api.domain.notificacao;

import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import com.skillswap.api.domain.notificacao.dto.AtualizarLeituraNotificacaoDTO;
import com.skillswap.api.domain.notificacao.dto.NotificacaoDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {
    private final NotificacaoService service;

    @GetMapping
    public ResponseEntity<PaginaDTO<NotificacaoDTO>> listar(
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int tamanho,
            Authentication authentication) {
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadaEm", "id"));
        return ResponseEntity.ok(PaginaDTO.de(service.listar((String) authentication.getPrincipal(), pageable)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<NotificacaoDTO> atualizarLeitura(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid AtualizarLeituraNotificacaoDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(service.atualizarLeitura(id, dto.lida(), (String) authentication.getPrincipal()));
    }
}
