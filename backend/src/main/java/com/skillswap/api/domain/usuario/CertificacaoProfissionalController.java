package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.usuario.dto.AtualizarCertificacaoProfissionalDTO;
import com.skillswap.api.domain.usuario.dto.CertificacaoProfissionalDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class CertificacaoProfissionalController {
    private final CertificacaoProfissionalService service;

    @GetMapping("/api/perfis/{id}/certificacoes")
    public List<CertificacaoProfissionalDTO> listarPublicas(@PathVariable @Positive Long id) {
        return service.listarPublicas(id);
    }

    @GetMapping("/api/certificacoes")
    public List<CertificacaoProfissionalDTO> listarProprias(Authentication auth) {
        return service.listarProprias((String) auth.getPrincipal());
    }

    @PostMapping("/api/certificacoes")
    public ResponseEntity<CertificacaoProfissionalDTO> criar(@RequestBody @Valid AtualizarCertificacaoProfissionalDTO dto, Authentication auth) {
        var criado = service.criar((String) auth.getPrincipal(), dto);
        return ResponseEntity.created(URI.create("/api/certificacoes/" + criado.id())).body(criado);
    }

    @PutMapping("/api/certificacoes/{id}")
    public ResponseEntity<CertificacaoProfissionalDTO> atualizar(@PathVariable @Positive Long id,
            @RequestBody @Valid AtualizarCertificacaoProfissionalDTO dto, Authentication auth) {
        return ResponseEntity.ok(service.atualizar(id, (String) auth.getPrincipal(), dto));
    }

    @DeleteMapping("/api/certificacoes/{id}")
    public ResponseEntity<Void> excluir(@PathVariable @Positive Long id, Authentication auth) {
        service.excluir(id, (String) auth.getPrincipal());
        return ResponseEntity.noContent().build();
    }
}
