package br.edu.unifio.skillswap.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @EqualsAndHashCode(of = "id")
public class ItemVitrine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, unique = true)
    private String codigo;
    @Column(nullable = false)
    private String grupo;
    @Column(nullable = false)
    private String titulo;
    @Column(nullable = false)
    private String imagem;
    private String busca;
    private String categoria;
    private int ordem;
}
