package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import com.skillswap.api.domain.usuario.dto.PerfilDTO;
import com.skillswap.api.domain.usuario.dto.UpdatePerfilDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Usuários. O cadastro (POST /api/usuarios) fica em AuthController porque também inicia a sessão.
 * Listagem e exclusão são restritas ao perfil ADMIN (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Validated
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<PaginaDTO<PerfilDTO>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        var pageable = PageRequest.of(pagina, tamanho, Sort.by("id"));
        return ResponseEntity.ok(PaginaDTO.de(usuarioService.listar(busca, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PerfilDTO> getPerfil(@PathVariable @Positive Long id) {
        var usuario = usuarioService.buscarPerfilPublico(id);
        return ResponseEntity.ok(usuarioService.toPerfilDTO(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PerfilDTO> updatePerfil(
            @PathVariable @Positive Long id,
            @RequestBody @Valid UpdatePerfilDTO dto,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var usuario = usuarioService.updateProfile(id, dto, email);
        return ResponseEntity.ok(usuarioService.toPerfilDTO(usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable @Positive Long id, Authentication authentication) {
        usuarioService.excluir(id, (String) authentication.getPrincipal());
        return ResponseEntity.noContent().build();
    }
}
