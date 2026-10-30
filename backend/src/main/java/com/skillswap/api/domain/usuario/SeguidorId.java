package com.skillswap.api.domain.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SeguidorId implements Serializable {
    @Column(name = "seguidor_id")
    private Long seguidorId;

    @Column(name = "seguido_id")
    private Long seguidoId;
}
