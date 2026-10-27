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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private static final int AMOSTRA_MINIMA_PARA_FILTRO_NOTA = 3;

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

    @Transactional(readOnly = true)
    public Page<PerfilDTO> buscarProfissionais(String busca, String cidade, Double notaMinima, Pageable pageable) {
        String termoSeguro = normalizarFiltro(busca, 100);
        String cidadeSegura = normalizarFiltro(cidade, 80);
        return usuarioRepository.buscarProfissionais(
                        termoSeguro, cidadeSegura, notaMinima, AMOSTRA_MINIMA_PARA_FILTRO_NOTA, pageable)
                .map(this::toPerfilDTO);
    }

    @Transactional
    public PerfilDTO atualizarPerfilAtual(UpdatePerfilDTO dto, String emailAutenticado) {
        Usuario usuario = findByEmail(emailAutenticado);
        aplicarAtualizacao(usuario, dto);
        return toPerfilDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public PerfilDTO atualizarFotoAtual(String fotoUrl, String emailAutenticado) {
        Usuario usuario = findByEmail(emailAutenticado);
        usuario.setFotoUrl(fotoUrl);
        return toPerfilDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void removerFotoAtual(String emailAutenticado) {
        Usuario usuario = findByEmail(emailAutenticado);
        usuario.setFotoUrl(null);
        usuarioRepository.save(usuario);
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
        if (dto.tituloProfissional() != null) usuario.setTituloProfissional(dto.tituloProfissional().strip());
        if (dto.bio() != null) usuario.setBio(dto.bio().strip());
        if (dto.cidade() != null) usuario.setCidade(dto.cidade().strip());
        if (dto.especialidades() != null) usuario.setEspecialidades(dto.especialidades().strip());
    }

    private String normalizarFiltro(String valor, int limite) {
        if (valor == null || valor.isBlank()) return "";
        String filtro = valor.strip().replaceAll("[%_\\\\]", "").replaceAll("\\s+", " ").strip();
        if (filtro.length() > limite) {
            throw new IllegalArgumentException("O filtro excede o limite permitido");
        }
        return filtro;
    }

    public Double getAverageRating(Long usuarioId) {
        var avaliacoes = avaliacaoRepository.findByAvaliadoIdAndModeradaOcultaFalse(usuarioId);
        if (avaliacoes.isEmpty()) return null;
        return avaliacoes.stream()
                .mapToInt(a -> a.getNota())
                .average()
                .orElse(0.0);
    }

    public PerfilDTO toPerfilDTO(Usuario u) {
        return new PerfilDTO(
                u.getId(), u.getNome(), u.getTituloProfissional(),
                u.getBio(), u.getCidade(), u.getFotoUrl(),
                u.getEspecialidades(), u.getCriadoEm(),
                getAverageRating(u.getId()), avaliacaoRepository.countByAvaliadoIdAndModeradaOcultaFalse(u.getId())
        );
    }
}
