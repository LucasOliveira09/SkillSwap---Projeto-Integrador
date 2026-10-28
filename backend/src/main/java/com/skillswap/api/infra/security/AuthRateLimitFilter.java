package com.skillswap.api.infra.security;

import com.skillswap.api.infra.exception.GlobalExceptionHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, TipoTentativaAuth> ROTAS = Map.of(
            "/api/sessoes", TipoTentativaAuth.LOGIN,
            "/api/auth/login", TipoTentativaAuth.LOGIN,
            "/api/usuarios", TipoTentativaAuth.CADASTRO,
            "/api/auth/registrar", TipoTentativaAuth.CADASTRO
    );

    private final LimitadorTentativasAuth limitador;
    private final ObjectMapper objectMapper;

    AuthRateLimitFilter(LimitadorTentativasAuth limitador, ObjectMapper objectMapper) {
        this.limitador = limitador;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || rota(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var resultado = limitador.registrar(rota(request), request.getRemoteAddr());
        if (!resultado.permitido()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(resultado.retryAfterSegundos()));
            response.setHeader("Cache-Control", "no-store");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            var erro = new GlobalExceptionHandler.Erro("MUITAS_REQUISICOES",
                    "Muitas tentativas. Tente novamente mais tarde.", Map.of());
            objectMapper.writeValue(response.getOutputStream(), erro);
            return;
        }
        chain.doFilter(request, response);
    }

    private static TipoTentativaAuth rota(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String caminho = contextPath == null || contextPath.isEmpty()
                ? uri
                : uri.substring(contextPath.length());
        return ROTAS.get(caminho);
    }
}
