package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.avaliacao.AvaliacaoRepository;
import com.skillswap.api.domain.usuario.dto.PerfilDTO;
import com.skillswap.api.domain.usuario.dto.RegisterDTO;
import com.skillswap.api.domain.usuario.dto.UpdatePerfilDTO;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario register(RegisterDTO dto) {
        if (dto.senha().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Senha deve ter no máximo 72 bytes.");
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }
        var usuario = Usuario.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .build();
        return usuarioRepository.save(usuario);
    }

    public Usuario findById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
    }

    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
    }

    @Transactional(readOnly = true)
    public PerfilDTO perfilAtual(String emailAutenticado) {
        return toPerfilDTO(findByEmail(emailAutenticado));
    }

    @Transactional
    public PerfilDTO atualizarPerfilAtual(UpdatePerfilDTO dto, String emailAutenticado) {
        Usuario usuario = findByEmail(emailAutenticado);
        aplicarAtualizacao(usuario, dto);
        return toPerfilDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public Usuario updateProfile(Long id, UpdatePerfilDTO dto, String emailAutenticado) {
        var usuario = findById(id);
        if (!usuario.getEmail().equals(emailAutenticado)) {
            throw new com.skillswap.api.infra.exception.NaoAutorizadoException("Não autorizado a editar este perfil");
        }
        aplicarAtualizacao(usuario, dto);
        return usuarioRepository.save(usuario);
    }

    private void aplicarAtualizacao(Usuario usuario, UpdatePerfilDTO dto) {
        if (dto.nome() != null) usuario.setNome(dto.nome().strip());
        if (dto.bio() != null) usuario.setBio(dto.bio().strip());
        if (dto.cidade() != null) usuario.setCidade(dto.cidade().strip());
        if (dto.fotoUrl() != null) usuario.setFotoUrl(dto.fotoUrl().strip());
        if (dto.especialidades() != null) usuario.setEspecialidades(dto.especialidades().strip());
    }

    public Double getAverageRating(Long usuarioId) {
        var avaliacoes = avaliacaoRepository.findByAvaliadoId(usuarioId);
        if (avaliacoes.isEmpty()) return null;
        return avaliacoes.stream()
                .mapToInt(a -> a.getNota())
                .average()
                .orElse(0.0);
    }

    public PerfilDTO toPerfilDTO(Usuario u) {
        return new PerfilDTO(
                u.getId(), u.getNome(),
                u.getBio(), u.getCidade(), u.getFotoUrl(),
                u.getEspecialidades(), u.getCriadoEm(),
                getAverageRating(u.getId()), avaliacaoRepository.countByAvaliadoId(u.getId())
        );
    }
}
