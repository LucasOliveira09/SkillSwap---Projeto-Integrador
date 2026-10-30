package com.skillswap.api.domain.notificacao;

import com.skillswap.api.domain.notificacao.dto.NotificacaoDTO;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificacaoService {
    private final NotificacaoRepository repository;
    private final UsuarioService usuarioService;

    @Transactional
    public void criar(Usuario destinatario, TipoNotificacao tipo, String titulo, String mensagem, String recursoUrl) {
        repository.save(Notificacao.builder()
                .destinatario(destinatario)
                .tipo(tipo)
                .titulo(titulo)
                .mensagem(mensagem)
                .recursoUrl(recursoUrl)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<NotificacaoDTO> listar(String emailUsuario, Pageable pageable) {
        Usuario usuario = usuarioService.findByEmail(emailUsuario);
        return repository.findByDestinatarioId(usuario.getId(), pageable).map(this::paraDto);
    }

    @Transactional
    public NotificacaoDTO atualizarLeitura(Long id, boolean lida, String emailUsuario) {
        Usuario usuario = usuarioService.findByEmail(emailUsuario);
        Notificacao notificacao = repository.findByIdAndDestinatarioId(id, usuario.getId())
                .orElseThrow(() -> new NaoEncontradoException("Notificação não encontrada"));
        notificacao.setLidaEm(lida ? (notificacao.getLidaEm() == null ? LocalDateTime.now() : notificacao.getLidaEm()) : null);
        return paraDto(notificacao);
    }

    private NotificacaoDTO paraDto(Notificacao notificacao) {
        return new NotificacaoDTO(notificacao.getId(), notificacao.getTipo(), notificacao.getTitulo(),
                notificacao.getMensagem(), notificacao.getRecursoUrl(), notificacao.getLidaEm() != null,
                notificacao.getCriadaEm());
    }
}
