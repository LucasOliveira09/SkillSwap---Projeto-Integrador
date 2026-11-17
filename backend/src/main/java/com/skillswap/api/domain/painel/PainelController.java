package com.skillswap.api.domain.painel;

import com.skillswap.api.domain.painel.dto.PainelPlataformaDTO;
import com.skillswap.api.domain.painel.dto.PainelUsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard operacional: /api/painel (qualquer conta) e /api/painel/plataforma (perfil ADMIN). */
@RestController
@RequestMapping("/api/painel")
@RequiredArgsConstructor
public class PainelController {
    private final PainelService painelService;

    @GetMapping
    public ResponseEntity<PainelUsuarioDTO> meuPainel(Authentication authentication) {
        return ResponseEntity.ok(painelService.doUsuario((String) authentication.getPrincipal()));
    }

    @GetMapping("/plataforma")
    public ResponseEntity<PainelPlataformaDTO> plataforma() {
        return ResponseEntity.ok(painelService.daPlataforma());
    }
}
