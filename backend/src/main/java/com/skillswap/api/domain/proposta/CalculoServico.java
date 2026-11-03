package com.skillswap.api.domain.proposta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Cálculo determinístico para tarifas previamente validadas da oferta. */
public final class CalculoServico {
    private static final BigDecimal MAXIMO = new BigDecimal("99999999.99");
    private static final BigDecimal MAXIMO_QUANTIDADE = new BigDecimal("999999.999");

    private CalculoServico() {}

    public record Componente(String descricao, BigDecimal tarifa, BigDecimal quantidade) {}
    public record ItemCalculado(String descricao, BigDecimal tarifa, BigDecimal quantidade, BigDecimal subtotal) {}
    public record Resultado(List<ItemCalculado> itens, BigDecimal subtotal, BigDecimal minimo, BigDecimal total) {}

    public static Resultado calcular(List<Componente> componentes, BigDecimal minimo) {
        if (componentes == null || componentes.isEmpty() || componentes.size() > 20) {
            throw new IllegalArgumentException("Informe entre 1 e 20 componentes de cobrança.");
        }
        BigDecimal minimoValidado = validarDinheiro(minimo == null ? BigDecimal.ZERO : minimo);
        List<ItemCalculado> itens = componentes.stream().map(componente -> {
            if (componente == null || componente.descricao() == null || componente.descricao().isBlank()
                    || componente.descricao().length() > 120) {
                throw new IllegalArgumentException("Descrição do componente inválida.");
            }
            BigDecimal tarifa = validarDinheiro(componente.tarifa());
            BigDecimal quantidade = componente.quantidade();
            if (quantidade == null || quantidade.signum() <= 0 || quantidade.scale() > 3
                    || quantidade.compareTo(MAXIMO_QUANTIDADE) > 0) {
                throw new IllegalArgumentException("Quantidade inválida.");
            }
            BigDecimal subtotal = tarifa.multiply(quantidade).setScale(2, RoundingMode.HALF_UP);
            if (subtotal.compareTo(MAXIMO) > 0) throw new IllegalArgumentException("Valor máximo excedido.");
            return new ItemCalculado(componente.descricao().strip(), tarifa, quantidade, subtotal);
        }).toList();
        BigDecimal subtotal = itens.stream().map(ItemCalculado::subtotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        if (subtotal.compareTo(MAXIMO) > 0) throw new IllegalArgumentException("Valor máximo excedido.");
        return new Resultado(itens, subtotal, minimoValidado, subtotal.max(minimoValidado));
    }

    private static BigDecimal validarDinheiro(BigDecimal valor) {
        if (valor == null || valor.signum() < 0 || valor.scale() > 2 || valor.compareTo(MAXIMO) > 0) {
            throw new IllegalArgumentException("Valor monetário inválido.");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }
}
