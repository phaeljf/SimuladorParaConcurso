package br.com.raphael.simuladorparaconcurso.servico;

import br.com.raphael.simuladorparaconcurso.dominio.Questao;
import br.com.raphael.simuladorparaconcurso.modelo.QuestaoExibicao;
import br.com.raphael.simuladorparaconcurso.repositorio.AreaConhecimentoRepositorio;
import br.com.raphael.simuladorparaconcurso.repositorio.QuestaoRepositorio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import br.com.raphael.simuladorparaconcurso.dominio.Dificuldade;
import br.com.raphael.simuladorparaconcurso.dominio.Escolaridade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SimuladoServico {

    private final QuestaoRepositorio questaoRepositorio;
    private final AreaConhecimentoRepositorio areaRepositorio;

    public SimuladoServico(QuestaoRepositorio questaoRepositorio,
                           AreaConhecimentoRepositorio areaRepositorio) {
        this.questaoRepositorio = questaoRepositorio;
        this.areaRepositorio = areaRepositorio;
    }

    /**
     * Monta a lista de QuestaoExibicao sorteando 'n' questões por área, respeitando a ordem
     * do LinkedHashMap (útil para manter a sequência de seleção do usuário).
     *
     * @param quantidadesPorArea Mapa (areaId -> quantidade)
     * @return Lista de QuestaoExibicao pronta para exibir no exame
     */

    public List<QuestaoExibicao> montarSimulado(LinkedHashMap<Long, Integer> quantidadesPorArea,
                                                Dificuldade dificuldade,
                                                Escolaridade escolaridade) {
        List<QuestaoExibicao> lista = new ArrayList<>();

        String dif = (dificuldade  != null ? dificuldade.name()  : null);
        String esc = (escolaridade != null ? escolaridade.name() : null);

        for (Map.Entry<Long, Integer> e : quantidadesPorArea.entrySet()) {
            Long areaId = e.getKey();
            int n = e.getValue();

            var area = areaRepositorio.findById(areaId).orElse(null);
            if (area == null || Boolean.FALSE.equals(area.getAtivo())) {
                throw new IllegalArgumentException("Área inválida ou inativa: " + areaId);
            }

            int disponiveis = questaoRepositorio.countByAreaConhecimentoIdAndAtivoTrue(areaId);
            if (disponiveis < n) {
                log.warn("Área {} possui apenas {} questões ativas, pedidas {}", areaId, disponiveis, n);
                n = disponiveis;
            }

            // CASO 1: comportamento "simples" (igual ao seu de antes)
            // → professor/usuário informou UMA dificuldade e/ou UMA escolaridade
            if (dif != null || esc != null) {
                List<Questao> sorteadas = questaoRepositorio.buscarAleatoriasPorAreaComFiltros(areaId, n, dif, esc);
                sorteadas.stream().map(QuestaoExibicao::de).forEach(lista::add);
                continue;
            }

            // CASO 2: modo PREDEFINIDO (dificuldade == null) + distribuição por escolaridade da prova
            // → quando dificuldade não foi informada, aplicamos 20/60/20
            var bucketsEsc = cotasEscolaridade(escolaridade, n); // pode gerar [ (ESC=null,n) ] se nivel não for informado

            for (var escPair : bucketsEsc) {
                Escolaridade escNivel = escPair.getKey(); // pode ser null (qualquer)
                int nEsc = escPair.getValue();
                if (nEsc <= 0) continue;

                // 20/60/20 dentro deste "balde" de escolaridade
                var bucketDif = cotasDificuldadePredefinido(nEsc);

                int faltando = nEsc;
                List<Questao> coletadas = new ArrayList<>();

                // 1) tenta por dificuldade dentro da escolaridade atual
                for (var difPair : bucketDif.entrySet()) {
                    int ped = Math.min(difPair.getValue(), Math.max(faltando, 0));
                    if (ped <= 0) continue;

                    String difStr = difPair.getKey().name();
                    String escStr = (escNivel != null ? escNivel.name() : null);

                    var chunk = questaoRepositorio.buscarAleatoriasPorAreaComFiltros(areaId, ped, difStr, escStr);
                    coletadas.addAll(chunk);
                    faltando -= chunk.size();
                }

                // 2) spillover: se ainda faltar, completa na MESMA escolaridade sem fixar dificuldade
                if (faltando > 0) {
                    String escStr = (escNivel != null ? escNivel.name() : null);
                    var extra = questaoRepositorio.buscarAleatoriasPorAreaComFiltros(areaId, faltando, null, escStr);
                    coletadas.addAll(extra);
                    faltando -= extra.size();
                }

                // 3) (opcional) se ainda faltar, pode relaxar escolaridade (deixa como está se não quiser relaxar)

                coletadas.stream().map(QuestaoExibicao::de).forEach(lista::add);
            }
        }

        return lista;
    }

    // ===== ANTIGO: mantém compatibilidade (sem filtros) =====
    public List<QuestaoExibicao> montarSimulado(LinkedHashMap<Long, Integer> quantidadesPorArea) {
        return montarSimulado(quantidadesPorArea, null, null);
    }


    /**
     * Corrige a prova contando acertos. Compara 'escolhida' vs 'correta' (ambas String: "A".."E").
     */
    public int corrigir(List<QuestaoExibicao> qs) {
        if (qs == null || qs.isEmpty()) return 0;

        int acertos = 0;
        for (QuestaoExibicao q : qs) {
            if (q == null) continue;
            String esc = q.getEscolhida(); // pode ser null
            String cor = q.getCorreta();   // "A".."E"
            if (esc != null && cor != null && cor.equalsIgnoreCase(esc)) {
                acertos++;
            }
        }
        return acertos;
    }

    // helpers no SimuladoServico (ou numa classe util interna)
    private int[] splitProporcional(int n, int... pesos) {
        // Ex.: n=5, pesos={20,60,20} => [1,3,1]
        double total = 0;
        for (int p : pesos) total += p;
        int[] base = new int[pesos.length];
        double[] frac = new double[pesos.length];
        int soma = 0;
        for (int i=0;i<pesos.length;i++) {
            double val = n * (pesos[i] / total);
            base[i] = (int) Math.floor(val);
            frac[i] = val - base[i];
            soma += base[i];
        }
        int rest = n - soma;
        while (rest-- > 0) {
            int best = 0;
            for (int i=1;i<pesos.length;i++) if (frac[i] > frac[best]) best = i;
            base[best]++; frac[best] = 0;
        }
        return base;
    }

    private List<Map.Entry<br.com.raphael.simuladorparaconcurso.dominio.Escolaridade,Integer>>
    cotasEscolaridade(br.com.raphael.simuladorparaconcurso.dominio.Escolaridade nivelProva, int n) {
        // regra: FUNDAMENTAL 100%; MEDIO 90/10; SUPERIOR 60/40
        switch (nivelProva) {
            case FUNDAMENTAL: return List.of(Map.entry(br.com.raphael.simuladorparaconcurso.dominio.Escolaridade.FUNDAMENTAL, n));
            case MEDIO: {
                int[] q = splitProporcional(n, 90, 10);
                return List.of(
                        Map.entry(br.com.raphael.simuladorparaconcurso.dominio.Escolaridade.MEDIO, q[0]),
                        Map.entry(br.com.raphael.simuladorparaconcurso.dominio.Escolaridade.FUNDAMENTAL, q[1])
                );
            }
            case SUPERIOR: {
                int[] q = splitProporcional(n, 60, 40);
                return List.of(
                        Map.entry(br.com.raphael.simuladorparaconcurso.dominio.Escolaridade.SUPERIOR, q[0]),
                        Map.entry(br.com.raphael.simuladorparaconcurso.dominio.Escolaridade.MEDIO, q[1])
                );
            }
            default: // fallback seguro
                return List.of(Map.entry(nivelProva, n));
        }
    }

    // para o PREDEFINIDO: 20% fácil, 60% moderada, 20% difícil
    private Map<br.com.raphael.simuladorparaconcurso.dominio.Dificuldade,Integer>
    cotasDificuldadePredefinido(int n) {
        int[] q = splitProporcional(n, 20, 60, 20);
        var map = new java.util.LinkedHashMap<br.com.raphael.simuladorparaconcurso.dominio.Dificuldade,Integer>();
        map.put(br.com.raphael.simuladorparaconcurso.dominio.Dificuldade.FACIL, q[0]);
        map.put(br.com.raphael.simuladorparaconcurso.dominio.Dificuldade.MODERADA, q[1]);
        map.put(br.com.raphael.simuladorparaconcurso.dominio.Dificuldade.DIFICIL, q[2]);
        return map;
    }

}
