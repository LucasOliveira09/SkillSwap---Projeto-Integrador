package com.skillswap.api.domain.anuncio;

import com.skillswap.api.domain.anuncio.dto.AnuncioRequestDTO;
import com.skillswap.api.domain.anuncio.dto.AnuncioResponseDTO;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnuncioService {

    private final AnuncioRepository anuncioRepository;
    private final UsuarioService usuarioService;

    @Transactional
    public Anuncio criar(AnuncioRequestDTO dto, String emailUsuario) {
        Usuario usuario = usuarioService.findByEmail(emailUsuario);
        var anuncio = Anuncio.builder()
                .titulo(dto.titulo())
                .descricao(dto.descricao())
                .categoria(dto.categoria())
                .cidade(dto.cidade())
                .modalidade(dto.modalidade())
                .preco(dto.preco())
                .habilidadeDesejada(dto.habilidadeDesejada())
                .usuario(usuario)
                .build();
        return anuncioRepository.save(anuncio);
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponseDTO> listar(CategoriaAnuncio categoria, String cidade, Pageable pageable) {
        return anuncioRepository.findByStatusAndFilters(StatusAnuncio.ATIVO, categoria, cidade, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public Anuncio buscarPorId(Long id) {
        return anuncioRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Anúncio não encontrado"));
    }

    @Transactional
    public Anuncio atualizar(Long id, AnuncioRequestDTO dto, String emailUsuario) {
        var anuncio = buscarPorId(id);
        checkOwner(anuncio, emailUsuario);
        anuncio.setTitulo(dto.titulo());
        anuncio.setDescricao(dto.descricao());
        anuncio.setCategoria(dto.categoria());
        anuncio.setCidade(dto.cidade());
        anuncio.setModalidade(dto.modalidade());
        anuncio.setPreco(dto.preco());
        anuncio.setHabilidadeDesejada(dto.habilidadeDesejada());
        return anuncioRepository.save(anuncio);
    }

    @Transactional
    public void inativar(Long id, String emailUsuario) {
        var anuncio = buscarPorId(id);
        checkOwner(anuncio, emailUsuario);
        anuncio.setStatus(StatusAnuncio.INATIVO);
        anuncioRepository.save(anuncio);
    }

    private void checkOwner(Anuncio anuncio, String emailUsuario) {
        if (!anuncio.getUsuario().getEmail().equals(emailUsuario)) {
            throw new NaoAutorizadoException("Você não é o dono deste anúncio");
        }
    }

    public AnuncioResponseDTO toResponseDTO(Anuncio a) {
        return new AnuncioResponseDTO(
                a.getId(), a.getTitulo(), a.getDescricao(),
                a.getCategoria(), a.getCidade(), a.getModalidade(),
                a.getPreco(), a.getHabilidadeDesejada(), a.getStatus(),
                a.getUsuario().getId(), a.getUsuario().getNome(),
                a.getCriadoEm()
        );
    }
}
