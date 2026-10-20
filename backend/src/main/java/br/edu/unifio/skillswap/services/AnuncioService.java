package br.edu.unifio.skillswap.services;

import br.edu.unifio.skillswap.dto.*;
import br.edu.unifio.skillswap.entities.Anuncio;
import br.edu.unifio.skillswap.repositories.*;
import java.text.Normalizer;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AnuncioService {
    private final AnuncioRepository anuncios;
    private final CategoriaRepository categorias;

    public static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).strip();
    }

    public static void indexar(Anuncio a) {
        a.setTextoBusca(normalizar(String.join(" ", a.getTitulo(), a.getDescricao(), a.getCidade(),
            a.getCategoria().getNome(), a.getHabilidadeDesejada(),
            a.getAutor() == null ? "" : a.getAutor(),
            a.getCategoriaExibicao() == null ? "" : a.getCategoriaExibicao())));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AnuncioResponse> listar(String busca, String categoria, int pagina, int tamanho) {
        String literal = normalizar(busca).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return PaginaResponse.de(anuncios.pesquisar(categoria, "%" + literal + "%", PageRequest.of(pagina, tamanho))
            .map(AnuncioResponse::de));
    }

    @Transactional(readOnly = true)
    public AnuncioResponse buscar(Integer id) {
        return AnuncioResponse.de(anuncios.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND)));
    }

    @Transactional
    public AnuncioResponse criar(AnuncioRequest dados) {
        var categoria = categorias.findByCodigo(dados.categoria())
            .orElseThrow(() -> new IllegalArgumentException("Selecione uma categoria válida."));
        boolean venda = dados.modalidade().equals("venda");
        if (venda && dados.preco() == null) throw new IllegalArgumentException("Informe o preço de venda.");
        if (!venda && (dados.habilidadeDesejada() == null || dados.habilidadeDesejada().isBlank())) {
            throw new IllegalArgumentException("Informe a habilidade desejada para troca.");
        }
        var anuncio = new Anuncio();
        anuncio.setTitulo(dados.titulo().strip());
        anuncio.setDescricao(dados.descricao().strip());
        anuncio.setCidade(dados.cidade().strip());
        anuncio.setCategoria(categoria);
        anuncio.setModalidade(dados.modalidade());
        anuncio.setPreco(venda ? dados.preco() : null);
        anuncio.setHabilidadeDesejada(venda ? "" : dados.habilidadeDesejada().strip());
        indexar(anuncio);
        return AnuncioResponse.de(anuncios.saveAndFlush(anuncio));
    }
}
