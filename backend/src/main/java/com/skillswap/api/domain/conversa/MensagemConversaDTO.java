package com.skillswap.api.domain.conversa;
import com.skillswap.api.domain.mensagem.Mensagem;
import java.time.LocalDateTime;
public record MensagemConversaDTO(Long id, Long conversaId, Long remetenteId, String remetenteNome,
        String conteudo, LocalDateTime enviadoEm) {
    public static MensagemConversaDTO de(Mensagem m) {
        return new MensagemConversaDTO(m.getId(), m.getProposta().getId(), m.getRemetente().getId(),
                m.getRemetente().getNome(), m.getConteudo(), m.getEnviadoEm());
    }
}
