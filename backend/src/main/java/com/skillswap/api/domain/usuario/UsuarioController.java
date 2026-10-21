package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.usuario.dto.PerfilDTO;
import com.skillswap.api.domain.usuario.dto.UpdatePerfilDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/{id}")
    public ResponseEntity<PerfilDTO> getPerfil(@PathVariable Long id) {
        var usuario = usuarioService.findById(id);
        return ResponseEntity.ok(usuarioService.toPerfilDTO(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PerfilDTO> updatePerfil(
            @PathVariable Long id,
            @RequestBody @Valid UpdatePerfilDTO dto,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var usuario = usuarioService.updateProfile(id, dto, email);
        return ResponseEntity.ok(usuarioService.toPerfilDTO(usuario));
    }
}
