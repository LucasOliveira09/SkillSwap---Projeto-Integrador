package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.usuario.dto.AtualizarExperienciaProfissionalDTO;
import com.skillswap.api.domain.usuario.dto.ExperienciaProfissionalDTO;
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
public class ExperienciaProfissionalController {
    private final ExperienciaProfissionalService service;

    @GetMapping("/api/perfis/{id}/experiencias")
    public List<ExperienciaProfissionalDTO> listarPublicas(@PathVariable @Positive Long id) {
        return service.listarPublicas(id);
    }

    @GetMapping("/api/experiencias")
    public List<ExperienciaProfissionalDTO> listarProprias(Authentication auth) {
        return service.listarProprias((String) auth.getPrincipal());
    }

    @PostMapping("/api/experiencias")
    public ResponseEntity<ExperienciaProfissionalDTO> criar(@RequestBody @Valid AtualizarExperienciaProfissionalDTO dto, Authentication auth) {
        var criado = service.criar((String) auth.getPrincipal(), dto);
        return ResponseEntity.created(URI.create("/api/experiencias/" + criado.id())).body(criado);
    }

    @PutMapping("/api/experiencias/{id}")
    public ResponseEntity<ExperienciaProfissionalDTO> atualizar(@PathVariable @Positive Long id,
            @RequestBody @Valid AtualizarExperienciaProfissionalDTO dto, Authentication auth) {
        return ResponseEntity.ok(service.atualizar(id, (String) auth.getPrincipal(), dto));
    }

    @DeleteMapping("/api/experiencias/{id}")
    public ResponseEntity<Void> excluir(@PathVariable @Positive Long id, Authentication auth) {
        service.excluir(id, (String) auth.getPrincipal());
        return ResponseEntity.noContent().build();
    }
}
