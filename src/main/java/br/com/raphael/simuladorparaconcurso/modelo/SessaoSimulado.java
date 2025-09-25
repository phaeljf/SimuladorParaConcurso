package br.com.raphael.simuladorparaconcurso.modelo;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter

public class SessaoSimulado {
    public String nomeCandidato;
    @Getter
    public List<QuestaoExibicao> questoes = new ArrayList<>();
    public int indiceAtual;
    public Instant fim; // horário de término ou null (sem limite)
    @Setter
    private List<EscolhaArea> escolhas;


    // === getters utilitários simples ===
    public String getNome() { return nomeCandidato; }        // para model.addAttribute("nome", ss.getNome())

    public int getTotal() { return questoes == null ? 0 : questoes.size(); }

    /** devolve a questão numa posição, com proteção de limites */
    public QuestaoExibicao getQuestao(int pos) {
        if (questoes == null || questoes.isEmpty()) return null;
        if (pos < 0) pos = 0;
        if (pos >= questoes.size()) pos = questoes.size() - 1;
        return questoes.get(pos);
    }

    /** milissegundos restantes ou null se sem limite */
    public Long restanteMs() {
        if (fim == null) return null;
        long ms = Duration.between(Instant.now(), fim).toMillis();
        return Math.max(ms, 0L);
    }

    /** Marca a alternativa da questão (índice/pos) */
    public void setEscolhida(int index, String alternativa) {
        if (questoes == null) return;
        if (index < 0 || index >= questoes.size()) return;
        questoes.get(index).setEscolhida(alternativa);
    }

    /** Resumo (total, acertos, erros) para a página de resultado */
    public Resumo gerarResumo() {
        int total = getTotal();
        int acertos = 0;
        if (questoes != null) {
            for (QuestaoExibicao q : questoes) {
                String esc = q.getEscolhida();
                String cor = q.getCorreta();
                if (esc != null && cor != null && cor.equalsIgnoreCase(esc)) acertos++;
            }
        }
        int erros = total - acertos;
        return new Resumo(total, acertos, erros);
    }



    public record Resumo(int total, int acertos, int erros) {}
}
