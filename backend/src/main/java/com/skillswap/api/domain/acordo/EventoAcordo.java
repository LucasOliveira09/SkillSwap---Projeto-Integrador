package com.skillswap.api.domain.acordo;

import com.skillswap.api.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_acordo", indexes = @Index(name = "idx_eventos_acordo_data", columnList = "acordo_id, criado_em, id"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoAcordo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acordo_id", nullable = false)
    private Acordo acordo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ator_id", nullable = false)
    private Usuario ator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoEventoAcordo tipo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
    }
}
