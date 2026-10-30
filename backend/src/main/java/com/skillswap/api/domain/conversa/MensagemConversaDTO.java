package com.skillswap.api.domain.conversa;

import com.skillswap.api.domain.anuncio.Anuncio;
import com.skillswap.api.domain.mensagem.Mensagem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MensagemConversaDTO(Long id, Long conversaId, Long propostaId, Long acordoId,
        Long anuncioId, String anuncioTitulo, String cidade, String modalidade, BigDecimal preco,
        Long remetenteId, String remetenteNome, String conteudo, LocalDateTime enviadoEm,
        PropostaMensagemDTO proposta) {
    public static MensagemConversaDTO de(Mensagem mensagem) {
        Anuncio anuncio = mensagem.getAnuncioContexto();
        if (anuncio == null && mensagem.getProposta() != null) anuncio = mensagem.getProposta().getAnuncio();
        if (anuncio == null && mensagem.getAcordo() != null) anuncio = mensagem.getAcordo().getProposta().getAnuncio();
        return new MensagemConversaDTO(
                mensagem.getId(),
                mensagem.getConversa() == null ? null : mensagem.getConversa().getId(),
                mensagem.getProposta() == null ? null : mensagem.getProposta().getId(),
                mensagem.getAcordo() == null ? null : mensagem.getAcordo().getId(),
                anuncio == null ? null : anuncio.getId(),
                anuncio == null ? null : anuncio.getTitulo(),
                anuncio == null ? null : anuncio.getCidade(),
                anuncio == null ? null : anuncio.getModalidade().name(),
                anuncio == null ? null : anuncio.getPreco(),
                mensagem.getRemetente().getId(), mensagem.getRemetente().getNome(),
                mensagem.getConteudo(), mensagem.getEnviadoEm(), PropostaMensagemDTO.de(mensagem.getProposta()));
    }
}
