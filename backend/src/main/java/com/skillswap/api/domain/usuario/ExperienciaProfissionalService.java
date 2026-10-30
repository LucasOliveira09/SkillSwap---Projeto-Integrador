package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.usuario.dto.AtualizarExperienciaProfissionalDTO;
import com.skillswap.api.domain.usuario.dto.ExperienciaProfissionalDTO;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExperienciaProfissionalService {
    private static final long LIMITE_POR_PERFIL = 50;
    private final ExperienciaProfissionalRepository experiencias;
    private final UsuarioRepository usuarios;

    @Transactional(readOnly = true)
    public List<ExperienciaProfissionalDTO> listarPublicas(Long usuarioId) {
        usuarios.findByIdAndPerfilPublicoTrue(usuarioId)
                .orElseThrow(() -> new NaoEncontradoException("Perfil não encontrado"));
        return listar(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<ExperienciaProfissionalDTO> listarProprias(String email) {
        var usuario = usuarios.findByEmail(email).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        return listar(usuario.getId());
    }

    @Transactional
    public ExperienciaProfissionalDTO criar(String email, AtualizarExperienciaProfissionalDTO dto) {
        var usuario = usuarios.findByEmailForUpdate(email).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        validarPeriodo(dto.dataInicio(), dto.dataFim());
        if (experiencias.countByUsuarioId(usuario.getId()) >= LIMITE_POR_PERFIL)
            throw new IllegalArgumentException("O perfil pode ter no máximo 50 experiências");
        var experiencia = ExperienciaProfissional.builder().usuario(usuario).build();
        aplicar(experiencia, dto);
        return ExperienciaProfissionalDTO.de(experiencias.save(experiencia));
    }

    @Transactional
    public ExperienciaProfissionalDTO atualizar(Long id, String email, AtualizarExperienciaProfissionalDTO dto) {
        var usuario = usuarios.findByEmail(email).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        var experiencia = experiencias.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new NaoEncontradoException("Experiência não encontrada"));
        validarPeriodo(dto.dataInicio(), dto.dataFim());
        aplicar(experiencia, dto);
        return ExperienciaProfissionalDTO.de(experiencias.save(experiencia));
    }

    @Transactional
    public void excluir(Long id, String email) {
        var usuario = usuarios.findByEmail(email).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        var experiencia = experiencias.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new NaoEncontradoException("Experiência não encontrada"));
        experiencias.delete(experiencia);
    }

    private List<ExperienciaProfissionalDTO> listar(Long usuarioId) {
        return experiencias.findByUsuarioIdOrderByDataInicioDescIdDesc(usuarioId).stream()
                .map(ExperienciaProfissionalDTO::de).toList();
    }

    private void validarPeriodo(java.time.LocalDate inicio, java.time.LocalDate fim) {
        if (fim != null && fim.isBefore(inicio)) throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial");
    }

    private void aplicar(ExperienciaProfissional experiencia, AtualizarExperienciaProfissionalDTO dto) {
        experiencia.setCargo(dto.cargo().strip());
        experiencia.setOrganizacao(dto.organizacao().strip());
        experiencia.setCidade(normalizar(dto.cidade()));
        experiencia.setDataInicio(dto.dataInicio());
        experiencia.setDataFim(dto.dataFim());
        experiencia.setDescricao(normalizar(dto.descricao()));
    }

    private String normalizar(String valor) { return valor == null || valor.isBlank() ? null : valor.strip(); }
}
