package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.usuario.dto.EstadoSeguindoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfis/{id}/seguidores/me")
@RequiredArgsConstructor
public class SeguidorController {
    private final SeguidorService service;

    @GetMapping
    public EstadoSeguindoDTO estado(@PathVariable Long id, Authentication auth) {
        return service.estado(id, (String) auth.getPrincipal());
    }

    @PutMapping
    public ResponseEntity<EstadoSeguindoDTO> seguir(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.seguir(id, (String) auth.getPrincipal()));
    }

    @DeleteMapping
    public ResponseEntity<EstadoSeguindoDTO> deixarDeSeguir(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.deixarDeSeguir(id, (String) auth.getPrincipal()));
    }
}
