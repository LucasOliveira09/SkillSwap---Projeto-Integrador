package br.edu.unifio.skillswap.controllers;

import br.edu.unifio.skillswap.services.VitrineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class VitrineController {
    private final VitrineService service;
    @GetMapping("/api/vitrine")
    public VitrineService.Vitrine listar() { return service.listar(); }
}
