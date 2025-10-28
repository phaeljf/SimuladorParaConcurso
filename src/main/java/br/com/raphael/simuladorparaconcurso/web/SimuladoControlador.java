package br.com.raphael.simuladorparaconcurso.web;

import br.com.raphael.simuladorparaconcurso.dominio.Prova;
import br.com.raphael.simuladorparaconcurso.dominio.ProvaQuestao;
import br.com.raphael.simuladorparaconcurso.modelo.*;
import br.com.raphael.simuladorparaconcurso.repositorio.AreaConhecimentoRepositorio;
import br.com.raphael.simuladorparaconcurso.repositorio.ProvaQuestaoRepositorio;
import br.com.raphael.simuladorparaconcurso.repositorio.ProvaRepositorio;
import br.com.raphael.simuladorparaconcurso.repositorio.QuestaoRepositorio;
import br.com.raphael.simuladorparaconcurso.servico.SimuladoServico;
import br.com.raphael.simuladorparaconcurso.servico.ServicoPdf;

import br.com.raphael.simuladorparaconcurso.web.dto.AreaQtdDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import br.com.raphael.simuladorparaconcurso.dominio.Dificuldade;
import br.com.raphael.simuladorparaconcurso.dominio.Escolaridade;


import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class SimuladoControlador {

    private final AreaConhecimentoRepositorio areaRepo;
    private final SimuladoServico simuladoServico;
    private final ProvaRepositorio provaRepositorio;
    private final ProvaQuestaoRepositorio provaQuestaoRepositorio;
    private final QuestaoRepositorio questaoRepo;

    // PDF
    private final SpringTemplateEngine templateEngine;
    private final ServicoPdf servicoPdf;

    // Landing (lista de áreas para configurar)
    @GetMapping("/configurar")
    public String configurar(Model model) {
        // 1) busca as áreas ativas (ajuste para seu finder real)
        var areas = areaRepo.findByAtivoTrueOrderByNomeAsc();

        // 2) monta DTO com a contagem
        var areasQtd = areas.stream()
                .map(a -> new AreaQtdDTO(
                        a.getId(),
                        a.getNome(),
                        a.getDescricao(), // ← voltou a descrição
                        (long) questaoRepo.countByAreaConhecimentoIdAndAtivoTrue(a.getId())
                ))
                .toList();

        // 3) envia para o template exatamente com o nome que o HTML usa
        model.addAttribute("areas", areasQtd);
        return "configurar";
    }


    @GetMapping("/")
    public String index() {
        return "index";
    }



    @PostMapping("/iniciar")
    public String iniciar(@RequestParam String nome,
                          @RequestParam(defaultValue = "0") int horas,
                          @RequestParam(defaultValue = "0") int minutos,
                          @RequestParam List<Long> areaId,
                          @RequestParam List<Integer> quantidade,
                          @RequestParam(required = false) Dificuldade dificuldade,
                          @RequestParam(required = false) Escolaridade escolaridade,
                          HttpSession session,
                          org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {

        // (area -> quantidade) preservando ordem
        LinkedHashMap<Long, Integer> mapa = new LinkedHashMap<>();
        for (int i = 0; i < areaId.size(); i++) {
            Integer q = quantidade.get(i);
            if (q != null && q > 0) mapa.put(areaId.get(i), q);
        }
        if (mapa.isEmpty()) return "redirect:/";

        // NOVO: validação de disponíveis por área
        for (Map.Entry<Long, Integer> e : mapa.entrySet()) {
            Long id = e.getKey();
            int solicitadas = e.getValue();
            int disponiveis = questaoRepo.countByAreaConhecimentoIdAndAtivoTrue(id);
            if (solicitadas > disponiveis) {
                ra.addFlashAttribute("erro",
                        "Área " + id + ": solicitou " + solicitadas + " mas só há " + disponiveis + " disponíveis.");
                return "redirect:/configurar";
            }
        }

        // sorteia e monta prova sem filtro
        List<QuestaoExibicao> questoes = simuladoServico.montarSimulado(mapa);

        // sorteia e monta com filtros de dificuldade e escolaridade == EM BREVE
        //List<QuestaoExibicao> questoes = simuladoServico.montarSimulado(mapa, dificuldade, escolaridade);

        // escolhas legíveis
        List<EscolhaArea> escolhas = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : mapa.entrySet()) {
            String nomeArea = areaRepo.findById(e.getKey()).orElseThrow().getNome();
            escolhas.add(new EscolhaArea(e.getKey(), nomeArea, e.getValue()));
        }

        // sessão
        SessaoSimulado s = new SessaoSimulado();
        s.setNomeCandidato(nome);
        s.setEscolhas(escolhas);
        s.setQuestoes(questoes);
        s.setIndiceAtual(0);

        long duracaoMin = (long) horas * 60 + minutos;
        s.setFim(duracaoMin > 0 ? Instant.now().plusSeconds(duracaoMin * 60) : null);

        session.setAttribute("sessaoSimulado", s);
        return "redirect:/simulado/q/1";
    }

    @GetMapping("/simulado")
    public String simulado(@RequestParam(required = false) Integer q, Model model, HttpSession session) {
        SessaoSimulado s = (SessaoSimulado) session.getAttribute("sessaoSimulado");
        if (s == null) return "redirect:/";
        if (s.getFim() != null && Instant.now().isAfter(s.getFim())) return "redirect:/resultado";

        if (q != null) s.setIndiceAtual(Math.max(0, Math.min(q, s.getQuestoes().size() - 1)));
        QuestaoExibicao atual = s.getQuestoes().get(s.getIndiceAtual());

        Long restanteMs = null;
        if (s.getFim() != null) restanteMs = Duration.between(Instant.now(), s.getFim()).toMillis();

        model.addAttribute("q", atual);
        model.addAttribute("pos", s.getIndiceAtual());
        model.addAttribute("total", s.getQuestoes().size());
        model.addAttribute("nome", s.getNomeCandidato());
        model.addAttribute("restanteMs", restanteMs);
        return "exam";
    }

    @PostMapping("/responder")
    public String responder(@RequestParam int pos,
                            @RequestParam(required = false) String alternativa,
                            @RequestParam String acao,
                            HttpSession session) {
        SessaoSimulado s = (SessaoSimulado) session.getAttribute("sessaoSimulado");
        if (s == null) return "redirect:/";
        if (s.getFim() != null && java.time.Instant.now().isAfter(s.getFim())) {
            return "redirect:/resultado";
        }

        // saneia pos dentro dos limites
        int total = (s.getQuestoes() == null) ? 0 : s.getQuestoes().size();
        if (total == 0) return "redirect:/configurar";
        pos = Math.max(0, Math.min(pos, total - 1));

        // normaliza e valida alternativas
        if (alternativa != null && !alternativa.isBlank()) {
            String alt = alternativa.substring(0, 1).toUpperCase(); // "A".."E"
            if ("A".equals(alt) || "B".equals(alt) || "C".equals(alt) || "D".equals(alt) || "E".equals(alt)) {
                s.getQuestoes().get(pos).setEscolhida(alt);
            }
        }

        // navegação
        switch (acao) {
            case "ant" -> s.setIndiceAtual(Math.max(pos - 1, 0));
            case "prox" -> s.setIndiceAtual(Math.min(pos + 1, total - 1));
            case "fim"  -> { return "redirect:/resultado"; }
            default     -> s.setIndiceAtual(pos); // sem ação válida, permanece
        }

        // redireciona para a rota RESTful
        return "redirect:/simulado/q/" + (s.getIndiceAtual() + 1);
    }


    @PostMapping("/finalizar")
    public String finalizar() {
        return "redirect:/resultado";
    }

    @GetMapping("/resultado")
    public String resultado(Model model, HttpSession session) {
        SessaoSimulado s = (SessaoSimulado) session.getAttribute("sessaoSimulado");
        if (s == null) return "redirect:/";
        preencherResumoResultado(model, s);
        return "result";
    }

    // ---------- PDF ----------
    @GetMapping("/resultado/pdf")
    public ResponseEntity<byte[]> baixarPdf(HttpServletRequest req,
                                            HttpServletResponse res,
                                            HttpSession session,
                                            Model model) {
        SessaoSimulado s = (SessaoSimulado) session.getAttribute("sessaoSimulado");
        if (s == null) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/").build();
        }

        // Reaproveita a mesma montagem do resultado
        preencherResumoResultado(model, s);

        // Base URL para resolver CSS/IMG no PDF
        String baseUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath().build().toUriString() + "/";

        // Thymeleaf Context “puro” (compatível com todas as versões)
        Context ctx = new Context(req.getLocale());
        ctx.setVariables(model.asMap());
        ctx.setVariable("baseUrl", baseUrl); // vamos usar no template

        String html = templateEngine.process("resultado-pdf", ctx);

        byte[] pdf = servicoPdf.htmlParaPdf(html, baseUrl);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resultado.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ---------- utilitário ----------
    private void preencherResumoResultado(Model model, SessaoSimulado s) {
        int acertos = simuladoServico.corrigir(s.getQuestoes());
        int total = s.getQuestoes().size();
        model.addAttribute("nome", s.getNomeCandidato());
        model.addAttribute("acertos", acertos);
        model.addAttribute("erros", total - acertos);
        model.addAttribute("total", total);
        model.addAttribute("questoes", s.getQuestoes());
    }


    @GetMapping("/provas/{id}")
    public String detalheProva(@PathVariable Long id,
                               @RequestParam(name = "code", required = false) String code,
                               HttpSession session,
                               Model model) {

        var prova = provaRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prova não encontrada"));

        boolean acesso = Boolean.TRUE.equals(prova.getPublica());

        // liberando via link: precisa estar habilitado, code bater e (se existir) não estar expirado
        if (!acesso
                && prova.isShareEnabled()
                && code != null
                && code.equals(prova.getShareCode())
                && (prova.getShareExpiresAt() == null || Instant.now().isBefore(prova.getShareExpiresAt()))) {

            @SuppressWarnings("unchecked")
            var granted = (Set<Long>) Optional.ofNullable(session.getAttribute("shareAccess"))
                    .orElseGet(() -> { var s = new HashSet<Long>(); session.setAttribute("shareAccess", s); return s; });
            granted.add(prova.getId());
            acesso = true;
        }

        // se já ganhou “passe” na sessão (clicou no link antes), também libera
        if (!acesso) {
            @SuppressWarnings("unchecked")
            var granted = (Set<Long>) session.getAttribute("shareAccess");
            if (granted != null && granted.contains(prova.getId())) {
                acesso = true;
            }
        }

        if (!acesso) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido");
        }

        // Carrega itens para mostrar “Distribuição por áreas” e total
        var itens = provaQuestaoRepositorio.findByProvaId(id);

        // total de questões
        model.addAttribute("totalQuestoes", itens.size());

        // monta lista de {areaNome, quantidade} ordenada por nome de área
        var porArea = itens.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        it -> it.getQuestao().getAreaConhecimento().getNome(),
                        java.util.stream.Collectors.counting()
                ));

        var areas = porArea.entrySet().stream()
                .map(e -> {
                    var m = new java.util.HashMap<String,Object>();
                    m.put("areaNome", e.getKey());
                    m.put("quantidade", e.getValue());
                    return m;
                })
                .sorted(java.util.Comparator.comparing(m -> (String) m.get("areaNome")))
                .toList();

        model.addAttribute("areas", areas);

        model.addAttribute("pv", prova);
        return "provas_publicas/detalhe";
    }



    @GetMapping("/provas/{id}/exame")
    public String abrirExame(@PathVariable Long id, Model model, HttpSession session) {
        Prova prova = provaRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prova não encontrada"));

        model.addAttribute("prova", prova);
        return "provas_publicas/exame"; // templates/provas_publicas/exame.html
    }



    @GetMapping("/simulado/q/{n}")
    public String mostrarQuestao(@PathVariable int n, Model model, HttpSession session) {
        SessaoSimulado s = (SessaoSimulado) session.getAttribute("sessaoSimulado");
        if (s == null || s.getQuestoes() == null || s.getQuestoes().isEmpty()) {
            return "redirect:/configurar"; // volta pro form se não houver sessão
        }

        int total = s.getQuestoes().size();
        int pos = Math.max(0, Math.min(n - 1, total - 1));

        // tempo restante (ms) ou null (sem limite)
        Long restanteMs = null;
        if (s.getFim() != null) {
            long ms = java.time.Duration.between(java.time.Instant.now(), s.getFim()).toMillis();
            restanteMs = Math.max(0, ms);
        }

        // pega a questão corrente (QuestaoExibicao) — seus getters são getA..getE(), getEnunciado(), getAreaNome(), getEscolhida()
        QuestaoExibicao qx = s.getQuestoes().get(pos);

        // DTO que o template consome (inclui E!)
        record Q(
                String enunciado,
                String a, String b, String c, String d, String e,
                String areaNome,
                String escolhida
        ) {}

        model.addAttribute("nome", s.getNomeCandidato());
        model.addAttribute("restanteMs", restanteMs);
        model.addAttribute("pos", pos);
        model.addAttribute("total", total);
        model.addAttribute("q", new Q(
                qx.getEnunciado(),
                qx.getA(), qx.getB(), qx.getC(), qx.getD(), qx.getE(),   // <- agora com E
                qx.getAreaNome(),
                qx.getEscolhida()
        ));

        s.setIndiceAtual(pos); // opcional: guardar posição atual
        return "exam"; // templates/exam.html
    }

    @PostMapping("/provas/{id}/iniciar")
    public String iniciarProvaExistente(@PathVariable Long id,
                                        @RequestParam String nome,
                                        HttpSession session) {

        var prova = provaRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prova não encontrada"));

        @SuppressWarnings("unchecked")
        var granted = (java.util.Set<Long>) session.getAttribute("shareAccess");
        boolean temPasse = (granted != null && granted.contains(prova.getId()));

        if (!Boolean.TRUE.equals(prova.getPublica()) && !temPasse) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não permitido");
        }

        var itens = provaQuestaoRepositorio.findByProvaId(id);
        if (itens == null || itens.isEmpty()) {
            // sem questões → volta ao detalhe
            return "redirect:/provas/" + id;
        }

        // Converte para QuestaoExibicao (já com A..E)
        var questoes = itens.stream()
                .map(br.com.raphael.simuladorparaconcurso.dominio.ProvaQuestao::getQuestao)
                .map(br.com.raphael.simuladorparaconcurso.modelo.QuestaoExibicao::de)
                .toList();

        // (opcional) embaralhar ordem:
        // questoes = new ArrayList<>(questoes); Collections.shuffle(questoes);

        var s = new br.com.raphael.simuladorparaconcurso.modelo.SessaoSimulado();
        s.setNomeCandidato(nome);
        s.setQuestoes(new java.util.ArrayList<>(questoes));
        s.setIndiceAtual(0);
        s.setEscolhas(java.util.Collections.emptyList()); // se não usar, pode omitir

        if (prova.getTempoMinutos() != null) {
            s.setFim(java.time.Instant.now().plusSeconds(prova.getTempoMinutos() * 60L));
        } else {
            s.setFim(null);
        }

        session.setAttribute("sessaoSimulado", s);
        return "redirect:/simulado/q/1"; // reaproveita o mesmo exam.html
    }

    @GetMapping("/provas/{id}/iniciar")
    public String iniciarGet(@PathVariable Long id) {
        return "redirect:/provas/" + id + "?erro=use_post";
    }

    // === ACESSO POR LINK COMPARTILHADO (SESSÃO) ===
    @SuppressWarnings("unchecked")
    private java.util.Set<Long> grantedIds(jakarta.servlet.http.HttpSession session) {
        Object att = session.getAttribute("shareAccess");
        if (att instanceof java.util.Set) return (java.util.Set<Long>) att;
        java.util.Set<Long> novo = new java.util.HashSet<>();
        session.setAttribute("shareAccess", novo);
        return novo;
    }

    private boolean temAcesso(br.com.raphael.simuladorparaconcurso.dominio.Prova prova,
                              jakarta.servlet.http.HttpSession session) {
        if (java.lang.Boolean.TRUE.equals(prova.getPublica())) return true;
        return grantedIds(session).contains(prova.getId());
    }



}
