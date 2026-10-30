package com.skillswap.api.domain.conversa;

import com.skillswap.api.domain.anuncio.Anuncio;
import com.skillswap.api.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversas", uniqueConstraints = @UniqueConstraint(name = "uk_conversas_par",
        columnNames = {"participante_menor_id", "participante_maior_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "participante_menor_id", nullable = false)
    private Usuario participanteMenor;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "participante_maior_id", nullable = false)
    private Usuario participanteMaior;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "anuncio_contexto_id")
    private Anuncio anuncioContexto;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "ultima_atividade_em", nullable = false)
    private LocalDateTime ultimaAtividadeEm;

    @Column(name = "ultima_lida_menor", nullable = false)
    @Builder.Default private long ultimaLidaMenor = 0;
    @Column(name = "ultima_leitura_menor_em")
    private LocalDateTime ultimaLeituraMenorEm;
    @Column(name = "ultima_lida_maior", nullable = false)
    @Builder.Default private long ultimaLidaMaior = 0;
    @Column(name = "ultima_leitura_maior_em")
    private LocalDateTime ultimaLeituraMaiorEm;

    @PrePersist
    protected void aoCriar() {
        var agora = LocalDateTime.now();
        if (criadoEm == null) criadoEm = agora;
        if (ultimaAtividadeEm == null) ultimaAtividadeEm = criadoEm;
    }
}
