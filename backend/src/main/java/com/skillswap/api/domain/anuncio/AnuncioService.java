package com.skillswap.api.domain.anuncio;

import com.skillswap.api.domain.anuncio.dto.AnuncioRequestDTO;
import com.skillswap.api.domain.anuncio.dto.AnuncioResponseDTO;
import com.skillswap.api.domain.anuncio.dto.DisponibilidadeAnuncioDTO;
import com.skillswap.api.domain.anuncio.dto.AtualizarStatusAnuncioDTO;
import com.skillswap.api.domain.avaliacao.AvaliacaoRepository;
import com.skillswap.api.domain.avaliacao.dto.ResumoAvaliacaoAnuncioDTO;
import com.skillswap.api.domain.usuario.Usuario;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.infra.exception.NaoAutorizadoException;
import com.skillswap.api.infra.exception.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AnuncioService {

    private final AnuncioRepository anuncioRepository;
    private final DisponibilidadeAnuncioRepository disponibilidadeRepository;
    private final UsuarioService usuarioService;
    private final AvaliacaoRepository avaliacaoRepository;

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
        substituirDisponibilidades(anuncio, dto.disponibilidades());
        return anuncioRepository.saveAndFlush(anuncio);
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponseDTO> listar(CategoriaAnuncio categoria, String cidade, Pageable pageable) {
        return mapearPaginaComAvaliacoes(anuncioRepository.findByStatusAndFilters(StatusAnuncio.ATIVO, categoria, cidade, pageable));
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponseDTO> listarOfertasPublicasDoUsuario(Long usuarioId, Pageable pageable) {
        return mapearPaginaComAvaliacoes(anuncioRepository.findByUsuarioIdAndStatusAndExemploFalse(
                usuarioId, StatusAnuncio.ATIVO, pageable));
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponseDTO> listarMeus(String emailUsuario, Pageable pageable) {
        var usuario = usuarioService.findByEmail(emailUsuario);
        return mapearPaginaComAvaliacoes(anuncioRepository.findByUsuarioIdAndExemploFalse(usuario.getId(), pageable));
    }

    @Transactional(readOnly = true)
    public Anuncio buscarMeu(Long id, String emailUsuario) {
        var anuncio = anuncioRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Anúncio não encontrado"));
        checkOwner(anuncio, emailUsuario);
        return anuncio;
    }

    @Transactional
    public Anuncio atualizarStatus(Long id, StatusAnuncio status, String emailUsuario) {
        var anuncio = anuncioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NaoEncontradoException("Anúncio não encontrado"));
        checkOwner(anuncio, emailUsuario);
        anuncio.setStatus(status);
        return anuncioRepository.save(anuncio);
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
        var anuncio = buscarMeu(id, emailUsuario);
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
        substituirDisponibilidades(anuncio, dto.disponibilidades());
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
    public Page<AnuncioResponseDTO> pesquisar(
            String categoria, String cidade, String modalidade, BigDecimal precoMinimo,
            BigDecimal precoMaximo, DiaSemana diaSemana, PeriodoDia periodo,
            String busca, Pageable pageable) {
        CategoriaAnuncio filtro = null;
        if (categoria != null && !categoria.isBlank()) {
            try { filtro = CategoriaAnuncio.valueOf(categoria.toUpperCase(java.util.Locale.ROOT)); }
            catch (IllegalArgumentException ex) { return Page.empty(pageable); }
        }
        ModalidadeAnuncio modalidadeFiltro = null;
        if (modalidade != null && !modalidade.isBlank()) {
            try { modalidadeFiltro = ModalidadeAnuncio.valueOf(modalidade.toUpperCase(java.util.Locale.ROOT)); }
            catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Modalidade inválida."); }
        }
        if (precoMinimo != null && precoMaximo != null && precoMinimo.compareTo(precoMaximo) > 0) {
            throw new IllegalArgumentException("O preço mínimo não pode ser maior que o preço máximo.");
        }
        String literal = Anuncio.normalizar(busca).replace("!","!!").replace("%","!%").replace("_","!_");
        return mapearPaginaComAvaliacoes(anuncioRepository.pesquisar(
                StatusAnuncio.ATIVO, filtro, cidade, modalidadeFiltro, precoMinimo, precoMaximo,
                diaSemana, periodo,
                "%" + literal + "%", pageable));
    }

    public AnuncioResponseDTO toResponseDTO(Anuncio a) {
        var resumo = avaliacaoRepository.resumirAnunciosAvaliados(List.of(a.getId())).stream().findFirst().orElse(null);
        var disponibilidade = disponibilidadesPorAnuncio(List.of(a.getId())).getOrDefault(a.getId(), List.of());
        return toResponseDTO(a, resumo, disponibilidade);
    }

    private Page<AnuncioResponseDTO> mapearPaginaComAvaliacoes(Page<Anuncio> pagina) {
        var ids = pagina.getContent().stream().map(Anuncio::getId).toList();
        Map<Long, ResumoAvaliacaoAnuncioDTO> resumos = ids.isEmpty() ? Map.of() :
                avaliacaoRepository.resumirAnunciosAvaliados(ids).stream()
                        .collect(Collectors.toMap(ResumoAvaliacaoAnuncioDTO::anuncioId, Function.identity()));
        Map<Long, List<DisponibilidadeAnuncioDTO>> disponibilidades = disponibilidadesPorAnuncio(ids);
        return pagina.map(anuncio -> toResponseDTO(anuncio, resumos.get(anuncio.getId()),
                disponibilidades.getOrDefault(anuncio.getId(), List.of())));
    }

    private AnuncioResponseDTO toResponseDTO(
            Anuncio a, ResumoAvaliacaoAnuncioDTO resumo, List<DisponibilidadeAnuncioDTO> disponibilidades) {
        return new AnuncioResponseDTO(
                a.getId(), a.getTitulo(), a.getDescricao(),
                a.getCategoria(), a.getCidade(), a.getModalidade(),
                a.getPreco(), a.getHabilidadeDesejada(), a.getStatus(),
                a.getUsuario().getId(), a.getUsuario().getNome(),
                a.getCriadoEm(), a.getCategoriaExibicao() == null ? a.getCategoria().rotulo() : a.getCategoriaExibicao(),
                a.getImagem(), a.getUnidade() == null ? "" : a.getUnidade(),
                a.getAutorExibicao() == null ? a.getUsuario().getNome() : a.getAutorExibicao(),
                a.getAvatar(), a.getEstrelas(), a.isExemplo(),
                resumo == null ? null : resumo.media(),
                resumo == null ? 0L : resumo.quantidade(),
                disponibilidades
        );
    }

    private Map<Long, List<DisponibilidadeAnuncioDTO>> disponibilidadesPorAnuncio(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return disponibilidadeRepository.buscarPorAnuncios(ids).stream()
                .collect(Collectors.groupingBy(
                        disponibilidade -> disponibilidade.getAnuncio().getId(),
                        Collectors.mapping(disponibilidade -> new DisponibilidadeAnuncioDTO(
                                disponibilidade.getDiaSemana(), disponibilidade.getPeriodo()),
                                Collectors.collectingAndThen(Collectors.toList(), this::ordenarDisponibilidades))));
    }

    private List<DisponibilidadeAnuncioDTO> ordenarDisponibilidades(List<DisponibilidadeAnuncioDTO> itens) {
        var ordenadas = new ArrayList<>(itens);
        ordenadas.sort(Comparator.comparing(DisponibilidadeAnuncioDTO::diaSemana)
                .thenComparing(DisponibilidadeAnuncioDTO::periodo));
        return List.copyOf(ordenadas);
    }

    private void substituirDisponibilidades(Anuncio anuncio, List<DisponibilidadeAnuncioDTO> declaradas) {
        List<DisponibilidadeAnuncioDTO> itens = declaradas == null ? List.of() : declaradas;
        Set<DisponibilidadeAnuncioDTO> unicas = new HashSet<>(itens);
        if (unicas.size() != itens.size()) {
            throw new IllegalArgumentException("Não repita o mesmo dia e período de disponibilidade.");
        }
        Set<DisponibilidadeAnuncio> entidades = itens.stream()
                .map(item -> DisponibilidadeAnuncio.builder()
                        .anuncio(anuncio)
                        .diaSemana(item.diaSemana())
                        .periodo(item.periodo())
                        .build())
                .collect(Collectors.toSet());
        anuncio.substituirDisponibilidades(entidades);
    }
}
