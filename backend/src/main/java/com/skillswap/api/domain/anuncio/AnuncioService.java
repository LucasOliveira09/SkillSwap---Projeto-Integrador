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
        validarNegociacao(dto);
        Usuario usuario = usuarioService.findByEmail(emailUsuario);
        var anuncio = Anuncio.builder()
                .titulo(dto.titulo().strip())
                .descricao(dto.descricao().strip())
                .categoria(dto.categoria())
                .cidade(dto.cidade().strip())
                .modalidade(dto.modalidade())
                .preco(dto.modalidade() == ModalidadeAnuncio.VENDA ? dto.preco() : null)
                .habilidadeDesejada(dto.modalidade() == ModalidadeAnuncio.TROCA ? dto.habilidadeDesejada().strip() : "")
                .imagem(dto.imagem() != null && !dto.imagem().isBlank() ? dto.imagem().strip() : null)
                .usuario(usuario)
                .build();
        return anuncioRepository.saveAndFlush(anuncio);
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponseDTO> listar(CategoriaAnuncio categoria, String cidade, Pageable pageable) {
        return anuncioRepository.findByStatusAndFilters(StatusAnuncio.ATIVO, categoria, cidade, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponseDTO> listarOfertasPublicasDoUsuario(Long usuarioId, Pageable pageable) {
        return anuncioRepository.findByUsuarioIdAndStatusAndExemploFalse(
                        usuarioId, StatusAnuncio.ATIVO, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public Anuncio buscarPorId(Long id) {
        return anuncioRepository.findById(id)
                .filter(anuncio -> anuncio.getStatus() == StatusAnuncio.ATIVO)
                .orElseThrow(() -> new NaoEncontradoException("Anúncio não encontrado"));
    }

    @Transactional(readOnly = true)
    public Anuncio buscarPorIdentificador(String idOuCodigo) {
        if (idOuCodigo == null || idOuCodigo.isBlank()) {
            throw new NaoEncontradoException("Anúncio não encontrado");
        }
        try {
            Long id = Long.parseLong(idOuCodigo);
            return buscarPorId(id);
        } catch (NumberFormatException e) {
            return anuncioRepository.findByCodigoLegado(idOuCodigo)
                    .filter(anuncio -> anuncio.getStatus() == StatusAnuncio.ATIVO)
                    .orElseThrow(() -> new NaoEncontradoException("Anúncio não encontrado"));
        }
    }

    @Transactional
    public Anuncio atualizar(Long id, AnuncioRequestDTO dto, String emailUsuario) {
        validarNegociacao(dto);
        var anuncio = buscarPorId(id);
        checkOwner(anuncio, emailUsuario);
        anuncio.setTitulo(dto.titulo().strip());
        anuncio.setDescricao(dto.descricao().strip());
        anuncio.setCategoria(dto.categoria());
        anuncio.setCidade(dto.cidade().strip());
        anuncio.setModalidade(dto.modalidade());
        anuncio.setPreco(dto.modalidade() == ModalidadeAnuncio.VENDA ? dto.preco() : null);
        anuncio.setHabilidadeDesejada(dto.modalidade() == ModalidadeAnuncio.TROCA ? dto.habilidadeDesejada().strip() : "");
        if (dto.imagem() != null) {
            anuncio.setImagem(dto.imagem().isBlank() ? null : dto.imagem().strip());
        }
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
        if (anuncio.isExemplo()) throw new NaoAutorizadoException("Exemplo somente para leitura.");
        if (!anuncio.getUsuario().getEmail().equals(emailUsuario)) {
            throw new NaoAutorizadoException("Você não é o dono deste anúncio");
        }
    }

    private void validarNegociacao(AnuncioRequestDTO dto) {
        if (dto.modalidade() == ModalidadeAnuncio.VENDA && dto.preco() == null)
            throw new IllegalArgumentException("Informe o preço de venda.");
        if (dto.modalidade() == ModalidadeAnuncio.TROCA &&
            (dto.habilidadeDesejada() == null || dto.habilidadeDesejada().isBlank()))
            throw new IllegalArgumentException("Informe a habilidade desejada.");
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponseDTO> pesquisar(String categoria, String cidade, String busca, Pageable pageable) {
        CategoriaAnuncio filtro = null;
        if (categoria != null && !categoria.isBlank()) {
            try { filtro = CategoriaAnuncio.valueOf(categoria.toUpperCase(java.util.Locale.ROOT)); }
            catch (IllegalArgumentException ex) { return Page.empty(pageable); }
        }
        String literal = Anuncio.normalizar(busca).replace("!","!!").replace("%","!%").replace("_","!_");
        return anuncioRepository.pesquisar(StatusAnuncio.ATIVO, filtro, cidade, "%"+literal+"%", pageable)
            .map(this::toResponseDTO);
    }

    public AnuncioResponseDTO toResponseDTO(Anuncio a) {
        return new AnuncioResponseDTO(
                a.getId(), a.getTitulo(), a.getDescricao(),
                a.getCategoria(), a.getCidade(), a.getModalidade(),
                a.getPreco(), a.getHabilidadeDesejada(), a.getStatus(),
                a.getUsuario().getId(), a.getUsuario().getNome(),
                a.getCriadoEm(), a.getCategoriaExibicao() == null ? a.getCategoria().rotulo() : a.getCategoriaExibicao(),
                a.getImagem(), a.getUnidade() == null ? "" : a.getUnidade(),
                a.getAutorExibicao() == null ? a.getUsuario().getNome() : a.getAutorExibicao(),
                a.getAvatar(), a.getEstrelas(), a.isExemplo()
        );
    }
}
