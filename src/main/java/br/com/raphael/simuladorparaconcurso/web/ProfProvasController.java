package br.com.raphael.simuladorparaconcurso.web;

import br.com.raphael.simuladorparaconcurso.dominio.Prova;
import br.com.raphael.simuladorparaconcurso.dominio.ProvaQuestao;
import br.com.raphael.simuladorparaconcurso.dominio.Questao;
import br.com.raphael.simuladorparaconcurso.modelo.ProfSessao;
import br.com.raphael.simuladorparaconcurso.repositorio.AreaConhecimentoRepositorio;
import br.com.raphael.simuladorparaconcurso.repositorio.ProvaQuestaoRepositorio;
import br.com.raphael.simuladorparaconcurso.repositorio.ProvaRepositorio;
import br.com.raphael.simuladorparaconcurso.repositorio.ProfessorRepositorio;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/prof/provas")
public class ProfProvasController {

    private final ProvaRepositorio provaRepo;
    private final ProvaQuestaoRepositorio provaQuestaoRepo;
    private final ProfessorRepositorio professorRepo;
    private final AreaConhecimentoRepositorio areaRepo;

    private ProfSessao sess(HttpSession s) { return (ProfSessao) s.getAttribute("PROF_AUTH"); }

    // ============== LISTAR (com filtro + paginação) ==============
    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size,
                         Model model, HttpSession session) {

        var prof = sess(session);
        if (prof == null) return "redirect:/prof/login";

        var pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50)
        );

        org.springframework.data.domain.Page<Prova> pagina =
                (q != null && !q.isBlank())
                        ? provaRepo.findByCriadoPorIdAndAtivoTrueAndTituloContainingIgnoreCaseOrderByCriadoEmDesc(
                        prof.id(), q.trim(), pageable)
                        : provaRepo.findByCriadoPorIdAndAtivoTrueOrderByCriadoEmDesc(
                        prof.id(), pageable);

        model.addAttribute("pagina", pagina);
        model.addAttribute("lista", pagina.getContent());
        model.addAttribute("f_q", q);
        return "prof/provas/list";
    }

    // ============== NOVA ==============
    @GetMapping("/nova")
    public String formNova(Model model, HttpSession session) {
        var prof = sess(session);
        if (prof == null) return "redirect:/prof/login";
        model.addAttribute("areas", areaRepo.findAll()); // filtros do painel de busca
        return "prof/provas/provas-form";
    }

    @PostMapping("/nova")
    @org.springframework.transaction.annotation.Transactional
    public String salvar(@RequestParam String titulo,
                         @RequestParam(required=false) String descricao,
                         @RequestParam(required=false) Integer tempoMinutos,
                         @RequestParam(defaultValue="false") boolean publica,
                         @RequestParam(name="mostrarGabarito", defaultValue="false") boolean mostrarGabarito,
                         @RequestParam(name="itens", required=false) List<Long> itens,
                         HttpSession session, RedirectAttributes ra) {

        var p = sess(session);
        if (p == null) return "redirect:/prof/login";

        var e = new Prova();
        e.setTitulo(titulo);
        e.setDescricao(descricao);
        if (tempoMinutos == null || tempoMinutos <= 0) tempoMinutos = 120; // fallback
        e.setTempoMinutos(tempoMinutos);
        e.setPublica(publica);
        e.setMostrarGabarito(mostrarGabarito);
        e.setCriadoPor(professorRepo.getReferenceById(p.id()));
        provaRepo.save(e);

        var ids = sanitizeIds(itens);
        if (!ids.isEmpty()) salvarItens(e, ids);

        ra.addFlashAttribute("ok", "Prova criada!");
        return "redirect:/prof/provas";
    }

    // ============== EDITAR ==============
    @GetMapping("/{id}/editar")
    public String formEditar(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        var p = sess(session);
        if (p == null) return "redirect:/prof/login";

        var prova = provaRepo.findByIdAndCriadoPorIdAndAtivoTrue(id, p.id()).orElse(null);
        if (prova == null) { ra.addFlashAttribute("erro","Prova não encontrada."); return "redirect:/prof/provas"; }

        model.addAttribute("pv", prova);
        model.addAttribute("areas", areaRepo.findAll());

        // Lista de IDs (ordem atual) para o form (sem CSV)
        var idsOrdenados = provaQuestaoRepo.findByProvaIdOrderByOrdemAsc(id)
                .stream().map(pq -> pq.getQuestao().getId()).toList();
        model.addAttribute("pvItens", idsOrdenados);

        return "prof/provas/provas-form";
    }

    @PostMapping("/{id}/editar")
    @org.springframework.transaction.annotation.Transactional
    public String salvarEd(@PathVariable Long id,
                           @RequestParam String titulo,
                           @RequestParam(required=false) String descricao,
                           @RequestParam(required=false) Integer tempoMinutos,
                           @RequestParam(defaultValue="false") boolean publica,
                           @RequestParam(name="mostrarGabarito", defaultValue="false") boolean mostrarGabarito,
                           @RequestParam(name="itens", required=false) List<Long> itens,
                           HttpSession session, RedirectAttributes ra) {

        var p = sess(session);
        if (p == null) return "redirect:/prof/login";

        var prova = provaRepo.findByIdAndCriadoPorIdAndAtivoTrue(id, p.id()).orElse(null);
        if (prova == null) { ra.addFlashAttribute("erro","Prova não encontrada."); return "redirect:/prof/provas"; }

        prova.setTitulo(titulo);
        prova.setDescricao(descricao);
        if (tempoMinutos == null || tempoMinutos <= 0) tempoMinutos = 120; // fallback
        prova.setTempoMinutos(tempoMinutos);
        prova.setPublica(publica);
        prova.setMostrarGabarito(mostrarGabarito);
        provaRepo.save(prova);

        // regrava itens na ordem recebida
        provaQuestaoRepo.deleteByProvaId(prova.getId());
        var ids = sanitizeIds(itens);
        if (!ids.isEmpty()) salvarItens(prova, ids);

        ra.addFlashAttribute("ok","Prova atualizada!");
        return "redirect:/prof/provas";
    }

    // ============== JSON: itens atuais da prova (usado no EDITAR para hidratar enunciados) ==============
    @GetMapping("/{id}/itens")
    @ResponseBody
    public ResponseEntity<?> itens(@PathVariable Long id, HttpSession session) {
        var p = sess(session);
        if (p == null) return ResponseEntity.status(401).build();

        var prova = provaRepo.findByIdAndCriadoPorIdAndAtivoTrue(id, p.id()).orElse(null);
        if (prova == null) return ResponseEntity.notFound().build();

        var itens = provaQuestaoRepo.findByProvaIdOrderByOrdemAsc(id).stream().map(pq -> {
            var q = pq.getQuestao();
            String area = (q.getAreaConhecimento() != null) ? q.getAreaConhecimento().getNome() : "Sem área";
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", q.getId());
            m.put("ordem", pq.getOrdem());
            m.put("enunciado", q.getEnunciado());
            m.put("area", area);
            m.put("publica", Boolean.TRUE.equals(q.getPublica()));
            return m;
        }).toList();

        return ResponseEntity.ok(Map.of("content", itens));
    }

    // ============== utilitários internos ==============
    private void salvarItens(Prova prova, List<Long> questaoIds) {
        int ordem = 1;
        for (Long qid : questaoIds) {
            // verificado que serve como blindagem extra par não ocorrer erro
            if (provaQuestaoRepo.existsByProvaIdAndQuestaoId(prova.getId(), qid)) continue;

            var pq = new ProvaQuestao();
            pq.setProva(prova);

            var q = new Questao();
            q.setId(qid);
            pq.setQuestao(q);

            pq.setOrdem(ordem++);
            provaQuestaoRepo.save(pq);
        }
    }

    @PostMapping("/{id}/excluir")
    @Transactional
    public String excluir(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        var p = sess(session);
        if (p == null) return "redirect:/prof/login";

        var prova = provaRepo.findByIdAndCriadoPorIdAndAtivoTrue(id, p.id()).orElse(null);
        if (prova == null) {
            ra.addFlashAttribute("erro", "Prova não encontrada.");
            return "redirect:/prof/provas";
        }

        // soft-delete
        prova.setAtivo(false);
        provaRepo.save(prova);

        ra.addFlashAttribute("ok", "Prova excluída.");
        return "redirect:/prof/provas";
    }

    private List<Long> sanitizeIds(List<Long> itens) {
        if (itens == null || itens.isEmpty()) return List.of();
        // preserva a ordem de chegada e remove nulos/duplicados
        LinkedHashSet<Long> set = new LinkedHashSet<>();
        for (Long id : itens) if (id != null) set.add(id);
        return new ArrayList<>(set);
    }
}
