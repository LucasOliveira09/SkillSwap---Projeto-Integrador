package com.skillswap.api.domain.avaliacao;

import com.skillswap.api.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contestacoes_avaliacao", uniqueConstraints = {
        @UniqueConstraint(name = "uk_contestacao_avaliacao", columnNames = "avaliacao_id")
}, indexes = {
        @Index(name = "idx_contestacao_status_data", columnList = "status, criada_em, id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContestacaoAvaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "avaliacao_id", nullable = false)
    private Avaliacao avaliacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contestador_id", nullable = false)
    private Usuario contestador;

    @Column(nullable = false, length = 1000)
    private String justificativa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusContestacaoAvaliacao status = StatusContestacaoAvaliacao.ABERTA;

    @Column(length = 1000)
    private String decisao;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm;

    @Column(name = "analisada_em")
    private LocalDateTime analisadaEm;

    @Column(name = "analisada_por", length = 320)
    private String analisadaPor;

    @PrePersist
    protected void onCreate() {
        this.criadaEm = LocalDateTime.now();
    }
}
