package com.skillswap.api.domain.moderacao;

import com.skillswap.api.domain.moderacao.dto.CriarDenunciaDTO;
import com.skillswap.api.domain.moderacao.dto.DecisaoModeracaoDTO;
import com.skillswap.api.domain.moderacao.dto.DenunciaDTO;
import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class ModeracaoController {
    private final ModeracaoService moderacaoService;

    @PostMapping("/api/denuncias")
    public ResponseEntity<DenunciaDTO> denunciar(@RequestBody @Valid CriarDenunciaDTO dto, Authentication auth) {
        var denuncia = moderacaoService.denunciar(dto, (String) auth.getPrincipal());
        return ResponseEntity.created(URI.create("/api/denuncias/" + denuncia.id())).body(denuncia);
    }

    @GetMapping("/api/denuncias/{id}")
    public ResponseEntity<DenunciaDTO> buscar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(moderacaoService.buscar(id, (String) auth.getPrincipal()));
    }

    @GetMapping("/api/moderacao/acesso")
    public ResponseEntity<Boolean> acesso(Authentication auth) {
        return ResponseEntity.ok(moderacaoService.podeModerar((String) auth.getPrincipal()));
    }

    @GetMapping("/api/moderacao/denuncias")
    public ResponseEntity<PaginaDTO<DenunciaDTO>> listar(
            @RequestParam(defaultValue = "ABERTA") StatusDenuncia status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int tamanho,
            Authentication auth) {
        return ResponseEntity.ok(moderacaoService.listar(status, pagina, tamanho, (String) auth.getPrincipal()));
    }

    @PatchMapping("/api/moderacao/denuncias/{id}")
    public ResponseEntity<DenunciaDTO> decidir(
            @PathVariable Long id, @RequestBody @Valid DecisaoModeracaoDTO dto, Authentication auth) {
        return ResponseEntity.ok(moderacaoService.decidir(id, dto, (String) auth.getPrincipal()));
    }
}
