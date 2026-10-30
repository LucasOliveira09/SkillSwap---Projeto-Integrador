package com.skillswap.api.domain.acordo;

import com.skillswap.api.domain.acordo.dto.EventoAcordoDTO;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventoAcordoService {
    private final EventoAcordoRepository eventos;
    private final AcordoRepository acordos;

    @Transactional
    public void registrar(Acordo acordo, Usuario ator, TipoEventoAcordo tipo) {
        eventos.save(EventoAcordo.builder().acordo(acordo).ator(ator).tipo(tipo).build());
    }

    @Transactional(readOnly = true)
    public List<EventoAcordoDTO> listar(Long acordoId, String emailUsuario) {
        Acordo acordo = acordos.findById(acordoId)
                .orElseThrow(() -> new NaoEncontradoException("Acordo não encontrado"));
        String anunciante = acordo.getProposta().getAnuncio().getUsuario().getEmail();
        String proponente = acordo.getProposta().getProponente().getEmail();
        if (!anunciante.equals(emailUsuario) && !proponente.equals(emailUsuario)) {
            throw new NaoAutorizadoException("Você não participa deste acordo");
        }
        return eventos.findByAcordoIdOrderByCriadoEmAscIdAsc(acordoId).stream()
                .map(evento -> new EventoAcordoDTO(evento.getId(), evento.getTipo(),
                        evento.getAtor().getId(), evento.getAtor().getNome(), evento.getCriadoEm()))
                .toList();
    }
}
