package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.usuario.dto.PerfilDTO;
import com.skillswap.api.domain.usuario.dto.UpdatePerfilDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PerfilController {

    private final UsuarioService usuarioService;

    @GetMapping("/perfis/{id}")
    public ResponseEntity<PerfilDTO> buscarPublico(@PathVariable Long id) {
        var usuario = usuarioService.findById(id);
        return ResponseEntity.ok(usuarioService.toPerfilDTO(usuario));
    }

    @GetMapping("/perfil")
    public ResponseEntity<PerfilDTO> buscarMeuPerfil(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(usuarioService.perfilAtual(email));
    }

    @PatchMapping("/perfil")
    public ResponseEntity<PerfilDTO> atualizarMeuPerfil(
            @RequestBody @Valid UpdatePerfilDTO dto,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(usuarioService.atualizarPerfilAtual(dto, email));
    }
}
