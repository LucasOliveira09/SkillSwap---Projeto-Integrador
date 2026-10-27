package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.usuario.dto.PerfilDTO;
import com.skillswap.api.domain.usuario.dto.UpdatePerfilDTO;
import com.skillswap.api.domain.anuncio.AnuncioService;
import com.skillswap.api.domain.anuncio.dto.AnuncioResponseDTO;
import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PerfilController {

    private final UsuarioService usuarioService;
    private final AnuncioService anuncioService;

    @GetMapping("/perfis/{id}")
    public ResponseEntity<PerfilDTO> buscarPublico(@PathVariable Long id) {
        var usuario = usuarioService.findById(id);
        return ResponseEntity.ok(usuarioService.toPerfilDTO(usuario));
    }

    @GetMapping("/perfis/{id}/anuncios")
    public ResponseEntity<PaginaDTO<AnuncioResponseDTO>> listarOfertas(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "6") @Min(1) @Max(20) int tamanho) {
        usuarioService.findById(id);
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm", "id"));
        return ResponseEntity.ok(PaginaDTO.de(anuncioService.listarOfertasPublicasDoUsuario(id, pageable)));
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
