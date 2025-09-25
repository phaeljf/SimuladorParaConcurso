package br.com.raphael.simuladorparaconcurso.modelo;

import br.com.raphael.simuladorparaconcurso.dominio.Questao;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QuestaoExibicao {
    private Long id;
    private String areaNome;
    private String enunciado;
    private String a, b, c, d, e; // 👈 agora com E (pode ser null)
    private String correta;       // "A".."E"
    @Setter
    private String escolhida;     // "A".."E" ou null


    public static QuestaoExibicao de(Questao q) {
        QuestaoExibicao v = new QuestaoExibicao();
        v.id = q.getId();
        v.areaNome = q.getAreaConhecimento() != null ? q.getAreaConhecimento().getNome() : "Sem área";
        v.enunciado = q.getEnunciado();

        // monta lista de textos (com E se existir)
        List<String> textos = new ArrayList<>(Arrays.asList(
                q.getAlternativaA(), q.getAlternativaB(), q.getAlternativaC(), q.getAlternativaD()
        ));
        boolean temE = q.getAlternativaE() != null && !q.getAlternativaE().isBlank();
        if (temE) textos.add(q.getAlternativaE());

        // índice da correta original (A..E -> 0..4)
        int idxCorretaOriginal = switch (Character.toUpperCase(q.getCorreta())) {
            case 'A' -> 0; case 'B' -> 1; case 'C' -> 2; case 'D' -> 3; case 'E' -> 4;
            default -> 0;
        };

        // ordem aleatória
        List<Integer> ordem = IntStream.range(0, textos.size()).boxed().collect(Collectors.toList());
        Collections.shuffle(ordem);

        // distribui para A..E conforme o tamanho
        v.a = textos.get(ordem.get(0));
        v.b = textos.get(1 < ordem.size() ? ordem.get(1) : 0);
        v.c = textos.get(2 < ordem.size() ? ordem.get(2) : 0);
        v.d = textos.get(3 < ordem.size() ? ordem.get(3) : 0);
        v.e = (temE ? textos.get(4 < ordem.size() ? ordem.get(4) : 0) : null);

        // nova posição da correta e letra correspondente
        int novaPosicaoCorreta = ordem.indexOf(idxCorretaOriginal);
        String letras = temE ? "ABCDE" : "ABCD";
        v.correta = String.valueOf(letras.charAt(Math.max(0, Math.min(novaPosicaoCorreta, letras.length()-1))));
        v.escolhida = null;
        return v;
    }

    // getters/setters usados no serviço/controlador
    public Long getId() { return id; }
    public String getAreaNome() { return areaNome; }
    public String getEnunciado() { return enunciado; }
    public String getA() { return a; }
    public String getB() { return b; }
    public String getC() { return c; }
    public String getD() { return d; }
    public String getE() { return e; }
    public String getCorreta() { return correta; }
    public String getEscolhida() { return escolhida; }
}
