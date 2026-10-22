package com.skillswap.api.domain.vitrine;

import com.skillswap.api.domain.anuncio.CategoriaAnuncio;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.*;

@RestController
public class CategoriaController {
    public record CategoriaDTO(String valor, String rotulo) {}
    @GetMapping("/api/categorias")
    public List<CategoriaDTO> listar() {
        return Arrays.stream(CategoriaAnuncio.values())
            .map(c -> new CategoriaDTO(c.name().toLowerCase(Locale.ROOT), c.rotulo())).toList();
    }
}
