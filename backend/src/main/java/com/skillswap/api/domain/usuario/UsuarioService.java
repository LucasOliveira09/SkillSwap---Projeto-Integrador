package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.avaliacao.AvaliacaoRepository;
import com.skillswap.api.domain.avaliacao.dto.ResumoAvaliacaoProfissionalDTO;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public Usuario buscarPerfilPublico(Long id) {
        return usuarioRepository.findByIdAndPerfilPublicoTrue(id)
                .orElseThrow(() -> new NaoEncontradoException("Perfil não encontrado"));
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
        Page<Usuario> profissionais = usuarioRepository.buscarProfissionais(
                termoSeguro, cidadeSegura, notaMinima, AMOSTRA_MINIMA_PARA_FILTRO_NOTA, pageable);
        List<Long> ids = profissionais.getContent().stream().map(Usuario::getId).toList();
        Map<Long, ResumoAvaliacaoProfissionalDTO> resumos = ids.isEmpty() ? Map.of()
                : avaliacaoRepository.resumirProfissionais(ids).stream()
                        .collect(Collectors.toMap(ResumoAvaliacaoProfissionalDTO::profissionalId, Function.identity()));
        return profissionais.map(profissional -> toPerfilDTO(profissional, resumos.get(profissional.getId())));
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
    public PerfilDTO atualizarCapaAtual(String capaUrl, String emailAutenticado) {
        Usuario usuario = findByEmail(emailAutenticado);
        usuario.setCapaUrl(capaUrl);
        return toPerfilDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void removerFotoAtual(String emailAutenticado) {
        Usuario usuario = findByEmail(emailAutenticado);
        usuario.setFotoUrl(null);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void removerCapaAtual(String emailAutenticado) {
        Usuario usuario = findByEmail(emailAutenticado);
        usuario.setCapaUrl(null);
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
        if (dto.linkedinUrl() != null) usuario.setLinkedinUrl(dto.linkedinUrl().strip());
        if (dto.instagramUrl() != null) usuario.setInstagramUrl(dto.instagramUrl().strip());
        if (dto.siteUrl() != null) usuario.setSiteUrl(dto.siteUrl().strip());
        if (dto.perfilPublico() != null) usuario.setPerfilPublico(dto.perfilPublico());
    }

    private String normalizarFiltro(String valor, int limite) {
        if (valor == null || valor.isBlank()) return "";
        String filtro = valor.strip().replaceAll("[%_\\\\]", "").replaceAll("\\s+", " ").strip();
        if (filtro.length() > limite) {
            throw new IllegalArgumentException("O filtro excede o limite permitido");
        }
        return filtro;
    }

    public PerfilDTO toPerfilDTO(Usuario u) {
        var resumo = avaliacaoRepository.resumirProfissionais(List.of(u.getId())).stream()
                .findFirst().orElse(null);
        return toPerfilDTO(u, resumo);
    }

    private PerfilDTO toPerfilDTO(Usuario u, ResumoAvaliacaoProfissionalDTO resumo) {
        return new PerfilDTO(
                u.getId(), u.getNome(), u.getTituloProfissional(),
                u.getBio(), u.getCidade(), u.getFotoUrl(),
                u.getCapaUrl(), u.getLinkedinUrl(), u.getInstagramUrl(), u.getSiteUrl(), u.isPerfilPublico(),
                u.getEspecialidades(), u.getCriadoEm(),
                resumo == null ? null : resumo.media(), resumo == null ? 0 : resumo.quantidade()
        );
    }
}
