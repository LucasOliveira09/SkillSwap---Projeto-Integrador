package com.skillswap.api.domain.portfolio;

import com.skillswap.api.domain.anuncio.Anuncio;
import com.skillswap.api.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publicacoes_portfolio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicacaoPortfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String legenda;

    @ElementCollection
    @CollectionTable(name = "publicacao_portfolio_fotos", joinColumns = @JoinColumn(name = "publicacao_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "foto_url", nullable = false, length = 120)
    @Builder.Default
    private List<String> fotos = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anuncio_id")
    private Anuncio anuncio;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void aoCriar() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
    }
}
