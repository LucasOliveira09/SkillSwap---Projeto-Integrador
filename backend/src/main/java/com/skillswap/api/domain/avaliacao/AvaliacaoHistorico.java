package com.skillswap.api.domain.avaliacao;

import com.skillswap.api.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_avaliacao", uniqueConstraints = @UniqueConstraint(
        name = "uk_historico_avaliacao", columnNames = "avaliacao_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvaliacaoHistorico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "avaliacao_id", nullable = false, updatable = false)
    private Avaliacao avaliacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "editor_id", nullable = false, updatable = false)
    private Usuario editor;

    @Column(name = "nota_anterior", nullable = false, updatable = false)
    private int notaAnterior;

    @Column(name = "nota_anuncio_anterior", updatable = false)
    private Integer notaAnuncioAnterior;

    @Column(name = "depoimento_anterior", length = 1000, updatable = false)
    private String depoimentoAnterior;

    @Column(name = "registrada_em", nullable = false, updatable = false)
    private LocalDateTime registradaEm;

    @PrePersist
    void aoCriar() {
        if (registradaEm == null) registradaEm = LocalDateTime.now();
    }
}
