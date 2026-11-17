package com.skillswap.api.infra.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Perfis de acesso da aplicação.
 * <ul>
 *   <li>USER: qualquer conta autenticada (anunciar, negociar, avaliar).</li>
 *   <li>ADMIN: equipe de moderação, definida por {@code skillswap.moderacao.emails}
 *       e, quando os usuários de demonstração estão ativos, pelo administrador de teste.</li>
 * </ul>
 */
@Component
public class PerfisAcesso {
    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    private final Set<String> administradores;

    public PerfisAcesso(
            @Value("${skillswap.moderacao.emails:}") String emailsModeradores,
            @Value("${skillswap.demo.enabled:false}") boolean demonstracaoAtiva,
            @Value("${skillswap.demo.admin-email:admin@skillswap.dev}") String emailAdminDemonstracao) {
        var emails = new HashSet<String>();
        Arrays.stream(emailsModeradores.split(","))
                .map(String::strip)
                .filter(e -> !e.isEmpty())
                .map(e -> e.toLowerCase(Locale.ROOT))
                .forEach(emails::add);
        if (demonstracaoAtiva) emails.add(emailAdminDemonstracao.toLowerCase(Locale.ROOT));
        this.administradores = Set.copyOf(emails);
    }

    public boolean administrador(String email) {
        return email != null && administradores.contains(email.toLowerCase(Locale.ROOT));
    }

    public List<GrantedAuthority> autoridades(String email) {
        return administrador(email)
                ? List.of(new SimpleGrantedAuthority("ROLE_" + USER), new SimpleGrantedAuthority("ROLE_" + ADMIN))
                : List.of(new SimpleGrantedAuthority("ROLE_" + USER));
    }
}
