package com.skillswap.api.domain.usuario;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(length = 500)
    private String bio;

    @Column(name = "titulo_profissional", length = 120)
    private String tituloProfissional;

    @Column(length = 80)
    private String cidade;

    private String fotoUrl;

    @Column(name = "capa_url", length = 120)
    private String capaUrl;

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    @Column(name = "instagram_url", length = 300)
    private String instagramUrl;

    @Column(name = "site_url", length = 300)
    private String siteUrl;

    @Column(length = 500)
    private String especialidades;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
    }
}
