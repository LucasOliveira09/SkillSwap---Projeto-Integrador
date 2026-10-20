package br.edu.unifio.skillswap.config;

import br.edu.unifio.skillswap.entities.Categoria;
import br.edu.unifio.skillswap.repositories.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
@RequiredArgsConstructor
public class CategoriasIniciais implements CommandLineRunner {
    private final CategoriaRepository categorias;

    @Override @Transactional
    public void run(String... args) {
        inserir("manutencao", "Manutenção e reparos");
        inserir("aulas", "Aulas e instrução");
        inserir("transporte", "Transporte local");
    }

    private void inserir(String codigo, String nome) {
        if (categorias.findByCodigo(codigo).isEmpty()) categorias.save(new Categoria(null, codigo, nome));
    }
}
