package com.skillswap.api.domain.comentario;

import com.skillswap.api.domain.anuncio.Anuncio;
import com.skillswap.api.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comentarios_anuncio")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComentarioAnuncio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anuncio_id", nullable = false)
    private Anuncio anuncio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resposta_a_id")
    private ComentarioAnuncio respostaA;

    @Column(nullable = false, length = 1000)
    private String conteudo;

    @Column(name = "moderada_oculta", nullable = false)
    private boolean moderadaOculta;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void aoCriar() { if (criadoEm == null) criadoEm = LocalDateTime.now(); }
}
