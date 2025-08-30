package br.com.raphael.simuladorparaconcurso.dominio;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.OffsetDateTime;

@Entity @Table(name="provas")
@Getter @Setter
public class Prova {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=120)
    private String titulo;

    @Column(columnDefinition="text")
    private String descricao;

    @Column(name="tempo_minutos")
    private Integer tempoMinutos;

    @Column(nullable=false)
    private Boolean publica = Boolean.TRUE;

    @Column(name="mostrar_gabarito", nullable=false)
    private Boolean mostrarGabarito = Boolean.TRUE;

    @Column(nullable=false)
    private Boolean ativo = Boolean.TRUE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="criado_por", nullable=false)
    private Professor criadoPor;

    @Column(name="criado_em")
    private OffsetDateTime criadoEm = OffsetDateTime.now();
}
