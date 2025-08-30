package br.com.raphael.simuladorparaconcurso.dominio;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Entity @Table(name="provas_questoes",
        uniqueConstraints = @UniqueConstraint(columnNames={"prova_id","questao_id"}))
@Getter @Setter
public class ProvaQuestao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="prova_id", nullable=false)
    private Prova prova;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="questao_id", nullable=false)
    private Questao questao;

    @Column(nullable=false)
    private Integer ordem;
}
