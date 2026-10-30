package com.skillswap.api.domain.proposta;

import com.skillswap.api.domain.proposta.dto.PropostaRequestDTO;
import com.skillswap.api.domain.proposta.dto.PropostaResponseDTO;
import com.skillswap.api.domain.proposta.dto.AtualizarStatusPropostaDTO;
import com.skillswap.api.domain.proposta.dto.PropostaConversaRequestDTO;
import com.skillswap.api.domain.anuncio.dto.PaginaDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PropostaController {

    private final PropostaService propostaService;

    @PostMapping("/conversas/{conversaId}/propostas")
    public ResponseEntity<PropostaResponseDTO> criarNaConversa(
            @PathVariable Long conversaId,
            @RequestBody @Valid PropostaConversaRequestDTO dto,
            Authentication authentication) {
        var proposta = propostaService.criarNaConversa(conversaId, dto, (String) authentication.getPrincipal());
        return ResponseEntity.created(java.net.URI.create("/api/propostas/" + proposta.getId()))
                .body(propostaService.toResponseDTO(proposta));
    }

    @PostMapping("/anuncios/{anuncioId}/propostas")
    public ResponseEntity<PropostaResponseDTO> criar(
            @PathVariable Long anuncioId,
            @RequestBody @Valid PropostaRequestDTO dto,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var proposta = propostaService.criar(anuncioId, dto, email);
        return ResponseEntity.created(java.net.URI.create("/api/propostas/" + proposta.getId()))
                .body(propostaService.toResponseDTO(proposta));
    }

    @GetMapping("/propostas")
    public ResponseEntity<PaginaDTO<PropostaResponseDTO>> listar(
            @RequestParam(defaultValue = "ENVIADAS") @Pattern(regexp = "(?i)ENVIADAS|RECEBIDAS") String direcao,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int tamanho,
            Authentication authentication) {
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm", "id"));
        var propostas = propostaService.listarPorDirecao(direcao, (String) authentication.getPrincipal(), pageable);
        return ResponseEntity.ok(PaginaDTO.de(propostas));
    }

    @GetMapping("/propostas/{id}")
    public ResponseEntity<PropostaResponseDTO> buscar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(propostaService.buscar(id, (String) authentication.getPrincipal()));
    }

    @PatchMapping("/propostas/{id}")
    public ResponseEntity<PropostaResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarStatusPropostaDTO dto,
            Authentication authentication) {
        var proposta = propostaService.atualizarStatus(id, dto, (String) authentication.getPrincipal());
        return ResponseEntity.ok(propostaService.toResponseDTO(proposta));
    }

    @GetMapping("/propostas/minhas")
    public ResponseEntity<List<PropostaResponseDTO>> listarMinhas(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(propostaService.listarEnviadas(email));
    }

    @GetMapping("/propostas/recebidas")
    public ResponseEntity<List<PropostaResponseDTO>> listarRecebidas(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        return ResponseEntity.ok(propostaService.listarRecebidas(email));
    }

    @PatchMapping("/propostas/{id}/aceitar")
    public ResponseEntity<PropostaResponseDTO> aceitar(
            @PathVariable Long id,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var proposta = propostaService.aceitar(id, email);
        return ResponseEntity.ok(propostaService.toResponseDTO(proposta));
    }

    @PatchMapping("/propostas/{id}/recusar")
    public ResponseEntity<PropostaResponseDTO> recusar(
            @PathVariable Long id,
            Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        var proposta = propostaService.recusar(id, email);
        return ResponseEntity.ok(propostaService.toResponseDTO(proposta));
    }
}
