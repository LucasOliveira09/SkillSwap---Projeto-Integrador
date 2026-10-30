package com.skillswap.api.domain.comentario;

import com.skillswap.api.domain.portfolio.PublicacaoPortfolio;
import com.skillswap.api.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comentarios_publicacao", indexes = @Index(
        name = "idx_comentario_publicacao_data", columnList = "publicacao_id, resposta_a_id, criado_em, id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComentarioPublicacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacao_id", nullable = false)
    private PublicacaoPortfolio publicacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resposta_a_id")
    private ComentarioPublicacao respostaA;

    @Column(nullable = false, length = 1000)
    private String conteudo;

    @Column(name = "moderada_oculta", nullable = false)
    private boolean moderadaOculta;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void aoCriar() { if (criadoEm == null) criadoEm = LocalDateTime.now(); }
}
