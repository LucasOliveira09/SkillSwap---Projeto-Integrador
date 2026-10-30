package com.skillswap.api.domain.upload;

import com.skillswap.api.domain.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "uploads_usuario", uniqueConstraints =
        @UniqueConstraint(name = "uk_uploads_usuario_arquivo", columnNames = {"usuario_id", "nome_arquivo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "nome_arquivo", nullable = false, length = 68)
    private String nomeArquivo;

    @Column(name = "tamanho_bytes", nullable = false)
    private long tamanhoBytes;

    @Column(name = "enviado_em", nullable = false, updatable = false)
    private LocalDateTime enviadoEm;

    @PrePersist
    protected void onCreate() {
        enviadoEm = LocalDateTime.now();
    }
}
