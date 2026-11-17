package com.skillswap.api.domain.painel;

import com.skillswap.api.domain.acordo.StatusAcordo;
import com.skillswap.api.domain.anuncio.StatusAnuncio;
import com.skillswap.api.domain.painel.dto.PainelPlataformaDTO;
import com.skillswap.api.domain.painel.dto.PainelUsuarioDTO;
import com.skillswap.api.domain.proposta.StatusProposta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Calcula os indicadores do dashboard a partir dos dados persistidos. */
@Service
@RequiredArgsConstructor
public class PainelService {
    private final PainelRepository painel;

    @Transactional(readOnly = true)
    public PainelUsuarioDTO doUsuario(String email) {
        long aceitas = painel.contarPropostasRecebidas(email, StatusProposta.ACEITA);
        long respondidas = painel.contarPropostasRecebidasRespondidas(email);
        return new PainelUsuarioDTO(
                painel.contarAnunciosDoUsuario(email, StatusAnuncio.ATIVO),
                painel.contarAnunciosDoUsuario(email, StatusAnuncio.INATIVO),
                painel.contarPropostasRecebidas(email, StatusProposta.PENDENTE),
                painel.contarPropostasEnviadas(email, StatusProposta.PENDENTE),
                painel.contarAcordosDoUsuario(email, StatusAcordo.EM_ANDAMENTO),
                painel.contarAcordosDoUsuario(email, StatusAcordo.CONCLUIDO),
                percentual(aceitas, respondidas),
                arredondar(painel.mediaNotasRecebidas(email)),
                painel.contarAvaliacoesRecebidas(email));
    }

    @Transactional(readOnly = true)
    public PainelPlataformaDTO daPlataforma() {
        Map<String, Long> acordos = mapa(painel.acordosPorStatus());
        long concluidos = acordos.getOrDefault(StatusAcordo.CONCLUIDO.name(), 0L);
        long encerrados = concluidos + acordos.getOrDefault(StatusAcordo.CANCELADO.name(), 0L);
        return new PainelPlataformaDTO(
                painel.contarUsuarios(),
                painel.contarAnuncios(StatusAnuncio.ATIVO),
                mapa(painel.propostasPorStatus()),
                acordos,
                percentual(concluidos, encerrados),
                arredondar(painel.mediaGeralAvaliacoes()),
                mapa(painel.anunciosAtivosPorCategoria()));
    }

    private static Map<String, Long> mapa(List<PainelRepository.ContagemPorChave> linhas) {
        var mapa = new LinkedHashMap<String, Long>();
        linhas.forEach(l -> mapa.put(String.valueOf(l.getChave()), l.getTotal()));
        return mapa;
    }

    private static Double percentual(long parte, long total) {
        return total == 0 ? null : arredondar(100.0 * parte / total);
    }

    private static Double arredondar(Double valor) {
        return valor == null ? null : BigDecimal.valueOf(valor).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
