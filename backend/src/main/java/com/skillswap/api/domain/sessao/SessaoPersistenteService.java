package com.skillswap.api.domain.sessao;

import com.skillswap.api.domain.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class SessaoPersistenteService {
    private final SessaoPersistenteRepository sessoes;
    private final UsuarioRepository usuarios;
    private final SecureRandom random = new SecureRandom();
    public static final int DIAS = 30;

    @Transactional
    public String criar(String email) {
        sessoes.deleteByExpiraEmBefore(agora());
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var sessao = new SessaoPersistente();
        sessao.setUsuario(usuarios.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Sessao invalida")));
        sessao.setTokenHash(hash(token));
        sessao.setExpiraEm(agora().plusDays(DIAS));
        sessoes.save(sessao);
        return token;
    }

    @Transactional(readOnly = true)
    public String email(String token) {
        if (!valido(token)) throw new BadCredentialsException("Sessao invalida");
        return sessoes.findByTokenHashAndExpiraEmAfter(hash(token), agora())
                .map(s -> s.getUsuario().getEmail())
                .orElseThrow(() -> new BadCredentialsException("Sessao invalida"));
    }

    @Transactional
    public void revogar(String token) {
        if (valido(token)) sessoes.deleteByTokenHash(hash(token));
    }

    private static boolean valido(String token) { return token != null && token.matches("[A-Za-z0-9_-]{43}"); }
    private static LocalDateTime agora() { return LocalDateTime.now(ZoneOffset.UTC); }
    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
