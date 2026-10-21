package com.skillswap.api.domain.acordo;

import com.skillswap.api.domain.proposta.Proposta;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "acordos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Acordo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposta_id", nullable = false)
    private Proposta proposta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusAcordo status = StatusAcordo.EM_ANDAMENTO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime concluidoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        if (this.status == null) {
            this.status = StatusAcordo.EM_ANDAMENTO;
        }
    }
}
