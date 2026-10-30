package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.usuario.dto.EstadoSeguindoDTO;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeguidorService {
    private final UsuarioRepository usuarios;
    private final SeguidorRepository seguidores;

    @Transactional(readOnly = true)
    public EstadoSeguindoDTO estado(Long seguidoId, String email) {
        var seguidor = usuarios.findByEmail(email)
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        var seguido = usuarios.findByIdAndPerfilPublicoTrue(seguidoId)
                .orElseThrow(() -> new NaoEncontradoException("Perfil não encontrado"));
        boolean proprio = seguidor.getId().equals(seguido.getId());
        return novoEstado(seguidor.getId(), seguido.getId(), proprio);
    }

    @Transactional
    public EstadoSeguindoDTO seguir(Long seguidoId, String email) {
        var seguidor = usuarios.findByEmail(email)
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        if (seguidor.getId().equals(seguidoId)) throw new IllegalArgumentException("Não é possível seguir o próprio perfil");
        var seguido = usuarios.findByIdForUpdate(seguidoId)
                .filter(Usuario::isPerfilPublico)
                .orElseThrow(() -> new NaoEncontradoException("Perfil não encontrado"));
        if (!seguidores.existsById_SeguidorIdAndId_SeguidoId(seguidor.getId(), seguidoId)) {
            seguidores.saveAndFlush(new Seguidor(seguidor, seguido));
        }
        return novoEstado(seguidor.getId(), seguidoId, false);
    }

    @Transactional
    public EstadoSeguindoDTO deixarDeSeguir(Long seguidoId, String email) {
        var seguidor = usuarios.findByEmail(email)
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        seguidores.deleteById_SeguidorIdAndId_SeguidoId(seguidor.getId(), seguidoId);
        return novoEstado(seguidor.getId(), seguidoId, seguidor.getId().equals(seguidoId));
    }

    private EstadoSeguindoDTO novoEstado(Long seguidorId, Long seguidoId, boolean proprio) {
        return new EstadoSeguindoDTO(!proprio && seguidores.existsById_SeguidorIdAndId_SeguidoId(seguidorId, seguidoId),
                proprio, seguidores.countById_SeguidoId(seguidoId));
    }
}
