package br.edu.unifio.skillswap.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @EqualsAndHashCode(of = "id")
public class Anuncio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, length = 100)
    private String titulo;
    @Column(nullable = false, length = 1500)
    private String descricao;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Categoria categoria;
    @Column(nullable = false, length = 80)
    private String cidade;
    @Column(nullable = false, length = 10)
    private String modalidade;
    @Column(precision = 10, scale = 2)
    private BigDecimal preco;
    @Column(nullable = false, length = 120)
    private String habilidadeDesejada = "";
    @Column(nullable = false, length = 2200)
    private String textoBusca;
    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
    @Column(unique = true, length = 50)
    private String codigoLegado;
    private String imagem;
    private String categoriaExibicao;
    private String unidade = "";
    private String autor;
    private String avatar;
    private Integer estrelas;
    private boolean exemplo;
}
