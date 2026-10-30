package com.skillswap.api.domain.proposta;

import com.skillswap.api.domain.anuncio.Anuncio;
import com.skillswap.api.domain.conversa.Conversa;
import com.skillswap.api.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "propostas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversa_id")
    private Conversa conversa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anuncio_id", nullable = false)
    private Anuncio anuncio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proponente_id", nullable = false)
    private Usuario proponente;

    @Column(nullable = false, length = 500)
    private String mensagemInicial;

    @Column(length = 120)
    private String servico;

    @Column(columnDefinition = "TEXT")
    private String escopo;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 20)
    private FormaPagamentoProposta formaPagamento;

    @Column(precision = 10, scale = 2)
    private BigDecimal valor;

    private Integer parcelas;

    @Column(length = 120)
    private String duracao;

    @Column(name = "validade_em")
    private LocalDate validadeEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusProposta status = StatusProposta.PENDENTE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "ultima_lida_anunciante", nullable = false)
    @Builder.Default
    private long ultimaLidaAnunciante = 0;
    @Column(name = "ultima_lida_proponente", nullable = false)
    @Builder.Default
    private long ultimaLidaProponente = 0;

    @Column(name = "ultima_leitura_anunciante_em")
    private LocalDateTime ultimaLeituraAnuncianteEm;
    @Column(name = "ultima_leitura_proponente_em")
    private LocalDateTime ultimaLeituraProponenteEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
        if (this.status == null) {
            this.status = StatusProposta.PENDENTE;
        }
    }
}
