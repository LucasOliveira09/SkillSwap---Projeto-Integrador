package com.skillswap.api.domain.vitrine;

import com.skillswap.api.domain.vitrine.ItemVitrine;
import com.skillswap.api.domain.vitrine.ItemVitrineRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VitrineService {
    private final ItemVitrineRepository itens;
    public record Item(String titulo, String imagem, Map<String, String> parametros) {
        static Item de(ItemVitrine item) {
            var parametros = new HashMap<String, String>();
            if (item.getBusca() != null) parametros.put("busca", item.getBusca());
            if (item.getCategoria() != null) parametros.put("categoria", item.getCategoria());
            return new Item(item.getTitulo(), item.getImagem(), parametros);
        }
    }
    public record Vitrine(List<Item> categorias, List<Item> destaques, List<Item> servicos) {}

    @Transactional(readOnly = true)
    public Vitrine listar() {
        var todos = itens.findAllByOrderByOrdemAscIdAsc();
        return new Vitrine(grupo(todos, "categorias"), grupo(todos, "destaques"), grupo(todos, "servicos"));
    }

    private List<Item> grupo(List<ItemVitrine> todos, String grupo) {
        return todos.stream().filter(item -> item.getGrupo().equals(grupo)).map(Item::de).toList();
    }
}
