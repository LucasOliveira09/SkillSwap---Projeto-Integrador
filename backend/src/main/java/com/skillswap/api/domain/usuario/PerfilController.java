package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.usuario.dto.PerfilDTO;
import com.skillswap.api.domain.usuario.dto.UpdatePerfilDTO;
import com.skillswap.api.domain.anuncio.AnuncioService;
import com.skillswap.api.domain.anuncio.dto.AnuncioResponseDTO;
import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import com.skillswap.api.domain.upload.UploadService;
import com.skillswap.api.domain.portfolio.PublicacaoPortfolioService;
import com.skillswap.api.domain.portfolio.dto.CriarPublicacaoPortfolioDTO;
import com.skillswap.api.domain.portfolio.dto.AtualizarLegendaPublicacaoDTO;
import com.skillswap.api.domain.portfolio.dto.PublicacaoPortfolioDTO;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PerfilController {

    private final UsuarioService usuarioService;
    private final AnuncioService anuncioService;
    private final UploadService uploadService;
    private final PublicacaoPortfolioService publicacaoPortfolioService;

    @GetMapping("/perfis/{id}")
    public ResponseEntity<PerfilDTO> buscarPublico(@PathVariable Long id) {
        var usuario = usuarioService.buscarPerfilPublico(id);
        return ResponseEntity.ok(usuarioService.toPerfilDTO(usuario));
    }

    @GetMapping("/perfis/{id}/anuncios")
    public ResponseEntity<PaginaDTO<AnuncioResponseDTO>> listarOfertas(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "6") @Min(1) @Max(20) int tamanho) {
        usuarioService.buscarPerfilPublico(id);
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm", "id"));
        return ResponseEntity.ok(PaginaDTO.de(anuncioService.listarOfertasPublicasDoUsuario(id, pageable)));
    }

    @GetMapping("/perfis/{id}/publicacoes")
    public ResponseEntity<PaginaDTO<PublicacaoPortfolioDTO>> listarPublicacoes(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "6") @Min(1) @Max(20) int tamanho) {
        usuarioService.buscarPerfilPublico(id);
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm", "id"));
        return ResponseEntity.ok(PaginaDTO.de(publicacaoPortfolioService.listar(id, pageable)));
    }

    @GetMapping("/publicacoes")
    public ResponseEntity<PaginaDTO<PublicacaoPortfolioDTO>> listarMinhasPublicacoes(
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "12") @Min(1) @Max(20) int tamanho,
            Authentication authentication) {
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm", "id"));
        return ResponseEntity.ok(PaginaDTO.de(publicacaoPortfolioService.listarProprias(
                (String) authentication.getPrincipal(), pageable)));
    }

    @org.springframework.web.bind.annotation.PostMapping("/publicacoes")
    public ResponseEntity<PublicacaoPortfolioDTO> criarPublicacao(
            @RequestBody @Valid CriarPublicacaoPortfolioDTO dto,
            Authentication authentication) {
        var publicacao = publicacaoPortfolioService.criar(dto, (String) authentication.getPrincipal());
        return ResponseEntity.created(java.net.URI.create("/api/publicacoes/" + publicacao.id())).body(publicacao);
    }

    @org.springframework.web.bind.annotation.GetMapping("/publicacoes/{id}")
    public ResponseEntity<PublicacaoPortfolioDTO> buscarPublicacao(@PathVariable Long id) {
        return ResponseEntity.ok(publicacaoPortfolioService.buscar(id));
    }

    @PatchMapping("/publicacoes/{id}")
    public ResponseEntity<PublicacaoPortfolioDTO> atualizarLegendaPublicacao(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarLegendaPublicacaoDTO dto,
            Authentication authentication) {
        var publicacao = publicacaoPortfolioService.atualizarLegenda(
                id, dto, (String) authentication.getPrincipal());
        return ResponseEntity.ok(publicacao);
    }

    @DeleteMapping("/publicacoes/{id}")
    public ResponseEntity<Void> excluirPublicacao(@PathVariable Long id, Authentication authentication) {
        publicacaoPortfolioService.excluir(id, (String) authentication.getPrincipal());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profissionais")
    public ResponseEntity<PaginaDTO<PerfilDTO>> buscarProfissionais(
            @RequestParam(defaultValue = "") @Size(max = 100) String busca,
            @RequestParam(defaultValue = "") @Size(max = 80) String cidade,
            @RequestParam(required = false) @DecimalMin("1.0") @DecimalMax("5.0") Double notaMinima,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "12") @Min(1) @Max(20) int tamanho) {
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm", "id"));
        return ResponseEntity.ok(PaginaDTO.de(usuarioService.buscarProfissionais(busca, cidade, notaMinima, pageable)));
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

    @PutMapping(value = "/perfil/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PerfilDTO> atualizarMinhaFoto(
            @RequestPart("file") MultipartFile arquivo,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var upload = uploadService.salvar(arquivo, email);
        return ResponseEntity.ok(usuarioService.atualizarFotoAtual(upload.url(), email));
    }

    @DeleteMapping("/perfil/foto")
    public ResponseEntity<Void> removerMinhaFoto(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        usuarioService.removerFotoAtual(email);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/perfil/capa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PerfilDTO> atualizarMinhaCapa(
            @RequestPart("file") MultipartFile arquivo,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var upload = uploadService.salvar(arquivo, email);
        return ResponseEntity.ok(usuarioService.atualizarCapaAtual(upload.url(), email));
    }

    @DeleteMapping("/perfil/capa")
    public ResponseEntity<Void> removerMinhaCapa(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        usuarioService.removerCapaAtual(email);
        return ResponseEntity.noContent().build();
    }
}
