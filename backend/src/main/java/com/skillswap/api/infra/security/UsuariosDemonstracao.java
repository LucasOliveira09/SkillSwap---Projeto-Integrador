package com.skillswap.api.infra.security;

import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria, de forma idempotente, as contas de teste usadas na avaliação da disciplina
 * (Postman e instruções de acesso). Ativado apenas com {@code DEMO_USERS_ENABLED=true}.
 * <ul>
 *   <li>admin: perfil ADMIN + USER (moderação, listagem e exclusão de usuários);</li>
 *   <li>profissional e cliente: perfil USER, usados no fluxo anúncio → proposta → acordo → avaliação.</li>
 * </ul>
 * As senhas vêm do ambiente; as padrões existem só para avaliação local e não devem ser usadas em produção.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "skillswap.demo.enabled", havingValue = "true")
public class UsuariosDemonstracao implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(UsuariosDemonstracao.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;

    @Value("${skillswap.demo.admin-email:admin@skillswap.dev}")
    private String emailAdmin;
    @Value("${skillswap.demo.admin-password}")
    private String senhaAdmin;
    @Value("${skillswap.demo.user-password}")
    private String senhaUsuarios;

    @Override
    @Transactional
    public void run(String... args) {
        if (senhaAdmin.isBlank() || senhaUsuarios.isBlank())
            throw new IllegalStateException("Defina DEMO_ADMIN_PASSWORD e DEMO_USER_PASSWORD.");
        garantir("Administrador SkillSwap", emailAdmin, senhaAdmin, "Equipe de moderação");
        garantir("Paula Profissional", "profissional@skillswap.dev", senhaUsuarios, "Professora de violão");
        garantir("Carlos Cliente", "cliente@skillswap.dev", senhaUsuarios, null);
        log.info("Usuários de demonstração disponíveis (ADMIN e USER).");
    }

    private void garantir(String nome, String email, String senha, String titulo) {
        var usuario = usuarios.findByEmail(email).orElseGet(() -> Usuario.builder()
                .nome(nome).email(email).build());
        usuario.setSenha(senhas.encode(senha));
        if (usuario.getTituloProfissional() == null) usuario.setTituloProfissional(titulo);
        if (usuario.getCidade() == null) usuario.setCidade("Florianópolis");
        usuarios.save(usuario);
    }
}
