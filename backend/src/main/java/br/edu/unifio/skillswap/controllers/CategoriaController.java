package br.edu.unifio.skillswap.controllers;

import br.edu.unifio.skillswap.repositories.CategoriaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CategoriaController {
    private final CategoriaRepository categorias;
    public record CategoriaResponse(String valor, String rotulo) {}

    @GetMapping("/api/categorias")
    public List<CategoriaResponse> listar() {
        return categorias.findAll(Sort.by("id")).stream()
            .map(c -> new CategoriaResponse(c.getCodigo(), c.getNome())).toList();
    }
}
