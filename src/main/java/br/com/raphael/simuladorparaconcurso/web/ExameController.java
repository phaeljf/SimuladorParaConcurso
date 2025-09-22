package br.com.raphael.simuladorparaconcurso.web;

import br.com.raphael.simuladorparaconcurso.modelo.SessaoSimulado;
import br.com.raphael.simuladorparaconcurso.modelo.QuestaoExibicao;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

//@Controller
@RequestMapping("/exam")
public class ExameController {

    // 1) Defina a chave da sessão aqui
    private static final String SESSION_KEY = "SESSAO_SIMULADO";

    // 2) Helper para pegar a sessão do exame
    private SessaoSimulado ss(HttpSession s) {
        return (SessaoSimulado) s.getAttribute(SESSION_KEY);
    }

    // 3) Exibe a questão atual (ou pela query 'pos')
    //    GET /exam?pos=0

    @GetMapping("/exam")
    public String exam(@RequestParam(defaultValue = "0") int pos,
                       HttpSession session,
                       Model model) {

        var ss = (SessaoSimulado) session.getAttribute("SESSAO_SIMULADO");
        if (ss == null) {
            return "redirect:/"; // se não tem sessão ativa, volta pro início
        }

        // pega a lista de questões da sessão
        var questoes = ss.getQuestoes();

        // valida índice
        if (pos < 0 || pos >= questoes.size()) {
            return "redirect:/result";
        }

        // pega a questão da posição atual
        var q = questoes.get(pos);

        // adiciona ao model para o Thymeleaf usar
        model.addAttribute("q", q);
        model.addAttribute("pos", pos);
        model.addAttribute("total", questoes.size());
        model.addAttribute("nome", ss.getNomeCandidato());
        model.addAttribute("restanteMs", ss.restanteMs());

        return "exam"; // exam.html
    }

    @PostMapping("/responder")
    public String responder(@RequestParam int pos,
                            @RequestParam(required=false) String alternativa,
                            @RequestParam String acao,       // "anterior" | "proximo" | "finalizar"
                            HttpSession session) {

        var ss = (SessaoSimulado) session.getAttribute("SESSAO_SIMULADO");
        if (ss == null) return "redirect:/";

        // grava escolha da questão atual (se vier)
        if (alternativa != null && !alternativa.isBlank()) {
            ss.setEscolhida(pos, alternativa.trim());
        }

        // navegação simples
        if ("anterior".equalsIgnoreCase(acao)) {
            pos = Math.max(0, pos - 1);
            return "redirect:/exam?pos=" + pos;
        } else if ("proximo".equalsIgnoreCase(acao)) {
            pos = Math.min(ss.getQuestoes().size() - 1, pos + 1);
            return "redirect:/exam?pos=" + pos;
        } else if ("finalizar".equalsIgnoreCase(acao)) {
            var resumo = ss.gerarResumo();
            session.setAttribute("RESUMO", resumo);
            return "redirect:/resultado"; // a rota/página que você quiser
        }

        return "redirect:/exam?pos=" + pos;
    }


    // 5) Mostra o resultado
    @GetMapping("/resultado")
    public String resultado(HttpSession session, Model model) {
        var ss = ss(session);
        if (ss == null || ss.getQuestoes() == null || ss.getQuestoes().isEmpty()) {
            return "redirect:/";
        }

        var r = ss.gerarResumo();
        model.addAttribute("nome", ss.getNomeCandidato());
        model.addAttribute("total", r.total());
        model.addAttribute("acertos", r.acertos());
        model.addAttribute("erros", r.erros());

        // Se quiser “encerrar” a sessão do exame, descomente:
        // session.removeAttribute(SESSION_KEY);

        // Reaproveite seu template existente (ou crie um) para o resultado:
        return "provas_publicas/resultado"; // ajuste para o caminho do seu template
    }
}
