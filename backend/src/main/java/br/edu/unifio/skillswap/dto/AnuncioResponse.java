package br.edu.unifio.skillswap.dto;

import br.edu.unifio.skillswap.entities.Anuncio;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AnuncioResponse(
    Integer id, String titulo, String descricao, String categoria, String categoriaRotulo,
    String cidade, String modalidade, BigDecimal preco, String habilidadeDesejada,
    LocalDateTime criadoEm, String imagem, String unidade, String autor, String avatar,
    Integer estrelas, boolean exemplo
) {
    public static AnuncioResponse de(Anuncio a) {
        return new AnuncioResponse(a.getId(), a.getTitulo(), a.getDescricao(), a.getCategoria().getCodigo(),
            a.getCategoriaExibicao() == null ? a.getCategoria().getNome() : a.getCategoriaExibicao(),
            a.getCidade(), a.getModalidade(), a.getPreco(), a.getHabilidadeDesejada(), a.getCriadoEm(),
            a.getImagem(), a.getUnidade(), a.getAutor(), a.getAvatar(), a.getEstrelas(), a.isExemplo());
    }
}
