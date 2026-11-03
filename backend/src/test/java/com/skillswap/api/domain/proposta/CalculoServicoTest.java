package com.skillswap.api.domain.proposta;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class CalculoServicoTest {
    private static BigDecimal valor(String valor) { return new BigDecimal(valor); }

    @Test void calculaComponentesArredondadosEAplicaMinimo() {
        var resultado = CalculoServico.calcular(List.of(
                new CalculoServico.Componente("Distância", valor("2.35"), valor("1.500")),
                new CalculoServico.Componente("Saída", valor("10.00"), valor("1"))), valor("20.00"));
        assertThat(resultado.itens().getFirst().subtotal()).isEqualByComparingTo("3.53");
        assertThat(resultado.subtotal()).isEqualByComparingTo("13.53");
        assertThat(resultado.total()).isEqualByComparingTo("20.00");
    }

    @Test void rejeitaValoresQuantidadesEExcessoDeComponentesInvalidos() {
        assertThatIllegalArgumentException().isThrownBy(() -> CalculoServico.calcular(
                List.of(new CalculoServico.Componente("Km", valor("1.001"), valor("1"))), null));
        assertThatIllegalArgumentException().isThrownBy(() -> CalculoServico.calcular(
                List.of(new CalculoServico.Componente("Km", valor("1"), valor("0"))), null));
        assertThatIllegalArgumentException().isThrownBy(() -> CalculoServico.calcular(
                List.of(new CalculoServico.Componente("Km", valor("99999999.99"), valor("2"))), null));
        assertThatIllegalArgumentException().isThrownBy(() -> CalculoServico.calcular(
                java.util.Collections.nCopies(21, new CalculoServico.Componente("Km", valor("1"), valor("1"))), null));
    }
}
