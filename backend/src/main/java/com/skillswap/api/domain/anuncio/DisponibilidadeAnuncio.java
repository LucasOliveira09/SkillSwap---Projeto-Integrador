package com.skillswap.api.domain.anuncio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "anuncio_disponibilidades", uniqueConstraints = @UniqueConstraint(
        name = "uk_anuncio_disponibilidade_dia_periodo",
        columnNames = {"anuncio_id", "dia_semana", "periodo"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DisponibilidadeAnuncio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anuncio_id", nullable = false)
    @ToString.Exclude
    private Anuncio anuncio;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 8)
    @EqualsAndHashCode.Include
    private DiaSemana diaSemana;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    @EqualsAndHashCode.Include
    private PeriodoDia periodo;

    @Builder
    public DisponibilidadeAnuncio(Anuncio anuncio, DiaSemana diaSemana, PeriodoDia periodo) {
        this.anuncio = anuncio;
        this.diaSemana = diaSemana;
        this.periodo = periodo;
    }
}
