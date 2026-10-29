package com.skillswap.api.infra.security;

import com.skillswap.api.domain.sessao.SessaoPersistenteService;
import com.skillswap.api.domain.usuario.dto.TokenDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessoes/atual")
@RequiredArgsConstructor
public class SessaoController {
    private final SessaoPersistenteService sessoes;
    private final SessaoCookie cookie;
    private final JwtService jwt;

    @PostMapping
    public ResponseEntity<TokenDTO> renovar(@CookieValue(name = SessaoCookie.NOME, required = false) String token,
            HttpServletRequest request, HttpServletResponse response) {
        SessaoCookie.exigirHeader(request);
        response.setHeader("Cache-Control", "no-store");
        if (token == null) return ResponseEntity.noContent().build();
        try { return ResponseEntity.ok(new TokenDTO(jwt.generateToken(sessoes.email(token)))); }
        catch (org.springframework.security.core.AuthenticationException ex) { cookie.limpar(response); throw ex; }
    }

    @DeleteMapping
    public ResponseEntity<Void> sair(@CookieValue(name = SessaoCookie.NOME, required = false) String token,
            HttpServletRequest request, HttpServletResponse response) {
        SessaoCookie.exigirHeader(request);
        sessoes.revogar(token);
        cookie.limpar(response);
        return ResponseEntity.noContent().build();
    }
}
