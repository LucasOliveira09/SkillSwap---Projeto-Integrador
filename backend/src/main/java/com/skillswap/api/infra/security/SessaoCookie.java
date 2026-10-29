package com.skillswap.api.infra.security;

import com.skillswap.api.domain.sessao.SessaoPersistenteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
public class SessaoCookie {
    public static final String NOME = "skillswap_session";
    public static final String HEADER = "X-SkillSwap-Session";
    private final SessaoPersistenteService sessoes;
    private final boolean secure;

    public SessaoCookie(SessaoPersistenteService sessoes,
                       @Value("${skillswap.security.session-cookie-secure:true}") boolean secure) {
        this.sessoes = sessoes;
        this.secure = secure;
    }

    public static void exigirHeader(HttpServletRequest request) {
        if (!"1".equals(request.getHeader(HEADER)) || "cross-site".equals(request.getHeader("Sec-Fetch-Site")))
            throw new org.springframework.security.access.AccessDeniedException("Requisicao de sessao invalida");
    }

    public void iniciar(String email, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        // Clientes Bearer existentes continuam compatíveis; só o cliente web solicita o cookie.
        if (!"1".equals(request.getHeader(HEADER))) return;
        exigirHeader(request);
        if (request.getCookies() != null) for (var c : request.getCookies())
            if (NOME.equals(c.getName())) sessoes.revogar(c.getValue());
        escrever(response, sessoes.criar(email), Duration.ofDays(SessaoPersistenteService.DIAS));
    }

    public void limpar(HttpServletResponse response) { escrever(response, "", Duration.ZERO); }
    private void escrever(HttpServletResponse response, String token, Duration duracao) {
        response.addHeader("Set-Cookie", ResponseCookie.from(NOME, token).httpOnly(true).secure(secure)
                .sameSite("Strict").path("/api/sessoes").maxAge(duracao).build().toString());
        response.setHeader("Cache-Control", "no-store");
    }
}
