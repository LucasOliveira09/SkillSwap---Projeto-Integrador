package com.skillswap.api.domain.moderacao;

import com.skillswap.api.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "denuncias", uniqueConstraints = @UniqueConstraint(
        name = "uk_denuncia_autor_recurso", columnNames = {"autor_id", "tipo_conteudo", "recurso_id"}),
        indexes = @Index(name = "ix_denuncias_status_criada", columnList = "status, criada_em, id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Denuncia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_conteudo", nullable = false, length = 32)
    private TipoConteudoDenunciado tipoConteudo;

    @Column(name = "recurso_id", nullable = false)
    private Long recursoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MotivoDenuncia motivo;

    @Column(length = 500)
    private String descricao;

    @Column(name = "trecho_conteudo", length = 1000)
    private String trechoConteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private StatusDenuncia status = StatusDenuncia.ABERTA;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm;

    @Column(name = "analisada_em")
    private LocalDateTime analisadaEm;

    @Column(name = "analisada_por", length = 255)
    private String analisadaPor;

    @PrePersist
    void aoCriar() {
        if (criadaEm == null) criadaEm = LocalDateTime.now();
    }
}
