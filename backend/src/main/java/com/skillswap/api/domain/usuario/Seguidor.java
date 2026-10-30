package com.skillswap.api.domain.usuario;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_seguidores")
@Getter
@Setter
@NoArgsConstructor
public class Seguidor {
    @EmbeddedId
    private SeguidorId id;

    @MapsId("seguidorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seguidor_id", nullable = false)
    private Usuario seguidor;

    @MapsId("seguidoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seguido_id", nullable = false)
    private Usuario seguido;

    private LocalDateTime criadoEm;

    public Seguidor(Usuario seguidor, Usuario seguido) {
        this.id = new SeguidorId(seguidor.getId(), seguido.getId());
        this.seguidor = seguidor;
        this.seguido = seguido;
        this.criadoEm = LocalDateTime.now();
    }
}
