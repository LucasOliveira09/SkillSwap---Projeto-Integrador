package com.skillswap.api.domain.anuncio;

import com.skillswap.api.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "anuncios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(nullable = false, length = 1500)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaAnuncio categoria;

    @Column(nullable = false, length = 80)
    private String cidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModalidadeAnuncio modalidade;

    @Column(precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(length = 120)
    private String habilidadeDesejada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusAnuncio status = StatusAnuncio.ATIVO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(unique = true, length = 50)
    private String codigoLegado;
    private String imagem;
    private String categoriaExibicao;
    private String unidade;
    private String autorExibicao;
    private String avatar;
    private Integer estrelas;
    @Builder.Default
    private boolean exemplo = false;
    @Column(length = 2200)
    private String textoBusca;

    @OneToMany(mappedBy = "anuncio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<DisponibilidadeAnuncio> disponibilidades = new HashSet<>();

    public void substituirDisponibilidades(Set<DisponibilidadeAnuncio> novas) {
        disponibilidades.clear();
        novas.forEach(disponibilidade -> {
            disponibilidade.setAnuncio(this);
            disponibilidades.add(disponibilidade);
        });
    }

    public static String normalizar(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT).strip();
    }

    @PreUpdate
    protected void indexar() {
        textoBusca = normalizar(String.join(" ", titulo, descricao, cidade, categoria.name(),
            habilidadeDesejada == null ? "" : habilidadeDesejada,
            categoriaExibicao == null ? "" : categoriaExibicao));
    }

    @PrePersist
    protected void onCreate() {
        if (this.criadoEm == null) this.criadoEm = LocalDateTime.now();
        indexar();
        if (this.status == null) {
            this.status = StatusAnuncio.ATIVO;
        }
    }
}
