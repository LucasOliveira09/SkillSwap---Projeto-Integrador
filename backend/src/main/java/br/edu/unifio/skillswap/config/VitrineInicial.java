package br.edu.unifio.skillswap.config;

import br.edu.unifio.skillswap.entities.*;
import br.edu.unifio.skillswap.repositories.*;
import br.edu.unifio.skillswap.services.AnuncioService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "skillswap.seed.enabled", havingValue = "true")
public class VitrineInicial implements CommandLineRunner {
    private final AnuncioRepository anuncios;
    private final CategoriaRepository categorias;
    private final ItemVitrineRepository itens;
    private final JsonMapper json;

    public record Oferta(String id, String titulo, String categoria, List<String> filtros, String imagem,
        BigDecimal preco, String unidade, String trocaPor, List<String> detalhes, String autor,
        String avatar, Integer estrelas, String quando, String cidade, boolean exemplo) {}
    public record Item(String titulo, String imagem, Map<String, String> parametros) {}
    public record Semente(List<Oferta> anuncios, List<Item> categorias, List<Item> destaques, List<Item> servicos) {}

    @Override @Transactional
    public void run(String... args) throws Exception {
        try (var arquivo = new ClassPathResource("vitrine-inicial.json").getInputStream()) {
            var semente = json.readValue(arquivo, Semente.class);
            for (int i = 0; i < semente.anuncios().size(); i++) inserirOferta(semente.anuncios().get(i), i);
            inserirItens("categorias", semente.categorias());
            inserirItens("destaques", semente.destaques());
            inserirItens("servicos", semente.servicos());
        }
    }

    private void inserirOferta(Oferta oferta, int ordem) {
        if (anuncios.existsByCodigoLegado(oferta.id())) return;
        var a = new Anuncio();
        a.setCodigoLegado(oferta.id());
        a.setTitulo(oferta.titulo());
        a.setDescricao(String.join("\n", oferta.detalhes()));
        a.setCategoria(categorias.findByCodigo(oferta.filtros().getFirst()).orElseThrow());
        a.setCategoriaExibicao(oferta.categoria());
        a.setCidade(oferta.cidade());
        a.setModalidade("venda");
        a.setPreco(oferta.preco());
        a.setImagem(oferta.imagem());
        a.setUnidade(oferta.unidade());
        a.setAutor(oferta.autor());
        a.setAvatar(oferta.avatar());
        a.setEstrelas(oferta.estrelas());
        a.setExemplo(true);
        a.setCriadoEm(LocalDateTime.of(2026, 10, 19, 0, 0).minusMinutes(ordem));
        AnuncioService.indexar(a);
        anuncios.save(a);
    }

    private void inserirItens(String grupo, List<Item> colecao) {
        for (int i = 0; i < colecao.size(); i++) {
            String codigo = grupo + "-" + i;
            if (itens.existsByCodigo(codigo)) continue;
            var origem = colecao.get(i);
            var item = new ItemVitrine();
            item.setCodigo(codigo);
            item.setGrupo(grupo);
            item.setOrdem(i);
            item.setTitulo(origem.titulo());
            item.setImagem(origem.imagem());
            item.setBusca(origem.parametros().get("busca"));
            item.setCategoria(origem.parametros().get("categoria"));
            itens.save(item);
        }
    }
}
