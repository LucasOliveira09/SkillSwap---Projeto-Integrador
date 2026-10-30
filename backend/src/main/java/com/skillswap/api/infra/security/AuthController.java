package com.skillswap.api.infra.security;

import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.domain.usuario.dto.LoginDTO;
import com.skillswap.api.domain.usuario.dto.RegisterDTO;
import com.skillswap.api.domain.usuario.dto.TokenDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final SessaoCookie sessaoCookie;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping({"/api/usuarios", "/api/auth/registrar"})
    public ResponseEntity<TokenDTO> registrar(@RequestBody @Valid RegisterDTO dto, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        if (request.getHeader(SessaoCookie.HEADER) != null) SessaoCookie.exigirHeader(request);
        var usuario = usuarioService.register(dto);
        var token = jwtService.generateToken(dto.email());
        sessaoCookie.iniciar(dto.email(), request, response);
        return ResponseEntity.created(java.net.URI.create("/api/usuarios/" + usuario.getId())).body(new TokenDTO(token));
    }

    @PostMapping({"/api/sessoes", "/api/auth/login"})
    public ResponseEntity<TokenDTO> login(@RequestBody @Valid LoginDTO dto, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        if (request.getHeader(SessaoCookie.HEADER) != null) SessaoCookie.exigirHeader(request);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.senha()));
        var token = jwtService.generateToken(dto.email());
        sessaoCookie.iniciar(dto.email(), request, response);
        return ResponseEntity.ok(new TokenDTO(token));
    }
}
