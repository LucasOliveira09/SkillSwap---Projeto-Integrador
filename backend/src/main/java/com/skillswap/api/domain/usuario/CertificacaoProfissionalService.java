package com.skillswap.api.domain.usuario;

import com.skillswap.api.domain.usuario.dto.AtualizarCertificacaoProfissionalDTO;
import com.skillswap.api.domain.usuario.dto.CertificacaoProfissionalDTO;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertificacaoProfissionalService {
    private static final long LIMITE_POR_PERFIL = 50;
    private final CertificacaoProfissionalRepository certificacoes;
    private final UsuarioRepository usuarios;

    @Transactional(readOnly = true)
    public List<CertificacaoProfissionalDTO> listarPublicas(Long usuarioId) {
        usuarios.findByIdAndPerfilPublicoTrue(usuarioId)
                .orElseThrow(() -> new NaoEncontradoException("Perfil não encontrado"));
        return listar(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<CertificacaoProfissionalDTO> listarProprias(String email) {
        var usuario = usuarios.findByEmail(email).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        return listar(usuario.getId());
    }

    @Transactional
    public CertificacaoProfissionalDTO criar(String email, AtualizarCertificacaoProfissionalDTO dto) {
        var usuario = usuarios.findByEmailForUpdate(email).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        validar(dto);
        if (certificacoes.countByUsuarioId(usuario.getId()) >= LIMITE_POR_PERFIL)
            throw new IllegalArgumentException("O perfil pode ter no máximo 50 certificações");
        var certificacao = CertificacaoProfissional.builder().usuario(usuario).build();
        aplicar(certificacao, dto);
        return CertificacaoProfissionalDTO.de(certificacoes.save(certificacao));
    }

    @Transactional
    public CertificacaoProfissionalDTO atualizar(Long id, String email, AtualizarCertificacaoProfissionalDTO dto) {
        var usuario = usuarios.findByEmail(email).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        var certificacao = certificacoes.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new NaoEncontradoException("Certificação não encontrada"));
        validar(dto);
        aplicar(certificacao, dto);
        return CertificacaoProfissionalDTO.de(certificacoes.save(certificacao));
    }

    @Transactional
    public void excluir(Long id, String email) {
        var usuario = usuarios.findByEmail(email).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado"));
        var certificacao = certificacoes.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new NaoEncontradoException("Certificação não encontrada"));
        certificacoes.delete(certificacao);
    }

    private List<CertificacaoProfissionalDTO> listar(Long usuarioId) {
        return certificacoes.findByUsuarioIdOrderByDataEmissaoDescIdDesc(usuarioId).stream()
                .map(CertificacaoProfissionalDTO::de).toList();
    }

    private void validar(AtualizarCertificacaoProfissionalDTO dto) {
        if (dto.dataValidade() != null && dto.dataValidade().isBefore(dto.dataEmissao()))
            throw new IllegalArgumentException("A validade deve ser igual ou posterior à emissão");
        if (dto.urlCredencial() != null && !dto.urlCredencial().isBlank()) {
            try {
                URI uri = URI.create(dto.urlCredencial().strip());
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
                    throw new IllegalArgumentException("A credencial deve usar uma URL HTTPS válida");
            } catch (IllegalArgumentException erro) {
                throw new IllegalArgumentException("A credencial deve usar uma URL HTTPS válida");
            }
        }
    }

    private void aplicar(CertificacaoProfissional certificacao, AtualizarCertificacaoProfissionalDTO dto) {
        certificacao.setNome(dto.nome().strip());
        certificacao.setEmissor(dto.emissor().strip());
        certificacao.setDataEmissao(dto.dataEmissao());
        certificacao.setDataValidade(dto.dataValidade());
        certificacao.setUrlCredencial(dto.urlCredencial() == null || dto.urlCredencial().isBlank() ? null : dto.urlCredencial().strip());
    }
}
