package com.skillswap.api.domain.mensagem;

import com.skillswap.api.domain.acordo.Acordo;
import com.skillswap.api.domain.proposta.Proposta;
import com.skillswap.api.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensagens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acordo_id")
    private Acordo acordo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposta_id")
    private Proposta proposta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuario remetente;

    @Column(nullable = false, length = 2000)
    private String conteudo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime enviadoEm;

    @PrePersist
    protected void onCreate() {
        this.enviadoEm = LocalDateTime.now();
    }
}
