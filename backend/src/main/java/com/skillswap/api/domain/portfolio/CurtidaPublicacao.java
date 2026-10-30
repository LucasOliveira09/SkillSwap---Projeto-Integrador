package com.skillswap.api.domain.portfolio;

import com.skillswap.api.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "curtidas_publicacao", uniqueConstraints = @UniqueConstraint(
        name = "uk_curtida_publicacao_usuario", columnNames = {"publicacao_id", "usuario_id"}),
        indexes = @Index(name = "idx_curtida_usuario_publicacao", columnList = "usuario_id, publicacao_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CurtidaPublicacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacao_id", nullable = false)
    private PublicacaoPortfolio publicacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void aoCriar() { if (criadoEm == null) criadoEm = LocalDateTime.now(); }
}
