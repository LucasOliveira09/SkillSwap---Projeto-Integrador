package com.skillswap.api.domain.vitrine;

import com.skillswap.api.domain.vitrine.VitrineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class VitrineController {
    private final VitrineService service;
    @GetMapping("/api/vitrine")
    public VitrineService.Vitrine listar() { return service.listar(); }
}
