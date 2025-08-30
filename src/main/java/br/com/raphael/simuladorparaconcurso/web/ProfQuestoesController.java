// ======================
// ProfQuestoesController
// ======================
// Pacote: mantenha exatamente este (de acordo com sua árvore existente).
package br.com.raphael.simuladorparaconcurso.web;

import br.com.raphael.simuladorparaconcurso.dominio.AreaConhecimento;
import br.com.raphael.simuladorparaconcurso.dominio.Questao;
import br.com.raphael.simuladorparaconcurso.modelo.ProfSessao; // record com id(), nome(), email()
import br.com.raphael.simuladorparaconcurso.repositorio.AreaConhecimentoRepositorio;
import br.com.raphael.simuladorparaconcurso.repositorio.QuestaoRepositorio;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;
import java.util.List;
import java.util.Optional;

/**
 * CRUD de Questões na área do professor.
 *
 * Rotas:
 *  - GET  /prof/questoes/minhas            : lista últimas 50 questões do professor
 *  - GET  /prof/questoes/nova              : form de criação
 *  - POST /prof/questoes/nova              : salva criação
 *  - GET  /prof/questoes/{id}/editar       : form de edição (apenas autor)
 *  - POST /prof/questoes/{id}/editar       : salva edição (apenas autor)
 *  - POST /prof/questoes/{id}/excluir      : exclui (apenas autor)
 *
 * Observações:
 *  - Usa "PROF_AUTH" na sessão para identificar o professor logado (ProfSessao).
 *  - Validações: enunciado e alternativas A–D obrigatórios; correta ∈ {A..E}.
 *  - Checkbox "publica": ausente = false; presente = true.
 *  - Em caso de conflito de unicidade (área + enunciado), retorna mensagem amigável.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/prof/questoes")
public class ProfQuestoesController {

    private final AreaConhecimentoRepositorio areaRepo;
    private final QuestaoRepositorio questaoRepo;

    // ========= util =========

    /** Recupera o professor da sessão (ou null se não logado). */
    private ProfSessao profFrom(HttpSession session) {
        return (ProfSessao) session.getAttribute("PROF_AUTH");
    }

    /** Se não logado, redireciona para login; caso contrário, retorna null. */
    private String requireLogin(ProfSessao prof) {
        return (prof == null ? "redirect:/prof/login" : null);
    }

    /** Valida os campos obrigatórios da questão e se 'correta' está entre A..E. */
    private String validar(String enunciado, String a, String b, String c, String d, String correta) {
        if (!StringUtils.hasText(enunciado)) return "Informe o enunciado.";
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b) || !StringUtils.hasText(c) || !StringUtils.hasText(d)) {
            return "Preencha as alternativas A, B, C e D.";
        }
        if (!StringUtils.hasText(correta)) return "Selecione a alternativa correta.";
        String cor = correta.trim().toUpperCase();
        if (!cor.matches("^[A-E]$")) return "A alternativa correta deve ser A, B, C, D ou E.";
        return null;
    }

    // ========= listar =========

    /**
     * Lista as últimas 50 questões do professor autenticado.
     * View: templates/prof/questoes/minhasquestoes.html
     */
    @GetMapping({"/minhas", "/minhasquestoes"})
    public String minhas(@RequestParam(required = false) String q,
                         @RequestParam(required = false) Long areaId,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size,
                         Model model, HttpSession session) {

        ProfSessao prof = (ProfSessao) session.getAttribute("PROF_AUTH");
        if (prof == null) return "redirect:/prof/login";

        var pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50)
        );

        org.springframework.data.domain.Page<Questao> pagina;

        boolean hasQ = (q != null && !q.isBlank());
        boolean hasArea = (areaId != null);

        if (hasQ && hasArea) {
            pagina = questaoRepo.findByAutorIdAndAtivoTrueAndAreaConhecimentoIdAndEnunciadoContainingIgnoreCaseOrderByIdDesc(prof.id(), areaId, q.trim(), pageable);
        } else if (hasQ) {
            pagina = questaoRepo.findByAutorIdAndAtivoTrueAndEnunciadoContainingIgnoreCaseOrderByIdDesc(prof.id(), q.trim(), pageable);
        } else if (hasArea) {
            pagina = questaoRepo.findByAutorIdAndAtivoTrueAndAreaConhecimentoIdOrderByIdDesc(prof.id(), areaId, pageable);
        } else {
            pagina = questaoRepo.findByAutorIdAndAtivoTrueOrderByIdDesc(prof.id(), pageable);
        }

        model.addAttribute("areas", areaRepo.findAll());
        model.addAttribute("pagina", pagina);
        model.addAttribute("lista", pagina.getContent()); // reaproveita o que já existia
        model.addAttribute("f_q", q);
        model.addAttribute("f_areaId", areaId);
        return "prof/questoes/minhasquestoes";
    }

    // ========= criar =========

    /**
     * Formulário de nova questão.
     * View: templates/prof/questoes/form.html
     */
    @GetMapping("/nova")
    public String formNova(Model model, HttpSession session) {
        ProfSessao prof = profFrom(session);
        String redirect = requireLogin(prof);
        if (redirect != null) return redirect;

        model.addAttribute("areas", areaRepo.findAll()); // usa o que você tem hoje
        return "prof/questoes/form";
    }

    /**
     * Recebe e salva nova questão.
     * Após sucesso redireciona para "Minhas Questões".
     */
    @PostMapping("/nova")
    public String salvarNova(@RequestParam Long areaId,
                             @RequestParam String enunciado,
                             @RequestParam String alternativaA,
                             @RequestParam String alternativaB,
                             @RequestParam String alternativaC,
                             @RequestParam String alternativaD,
                             @RequestParam(required = false) String alternativaE,
                             @RequestParam String correta,
                             @RequestParam(name = "publica", defaultValue = "false") boolean publica,
                             HttpSession session,
                             RedirectAttributes ra,
                             Model model) {
        ProfSessao prof = profFrom(session);
        String redirect = requireLogin(prof);
        if (redirect != null) return redirect;

        String erro = validar(enunciado, alternativaA, alternativaB, alternativaC, alternativaD, correta);
        if (erro != null) {
            ra.addFlashAttribute("erro", erro);
            return "redirect:/prof/questoes/nova";
        }

        Optional<AreaConhecimento> areaOpt = areaRepo.findById(areaId);
        if (areaOpt.isEmpty()) {
            ra.addFlashAttribute("erro", "Área inválida.");
            return "redirect:/prof/questoes/nova";
        }

        char letraCorreta = Character.toUpperCase(correta.trim().charAt(0));

        Questao q = new Questao();
        q.setAreaConhecimento(areaOpt.get());
        q.setEnunciado(enunciado);
        q.setAlternativaA(alternativaA);
        q.setAlternativaB(alternativaB);
        q.setAlternativaC(alternativaC);
        q.setAlternativaD(alternativaD);
        q.setAlternativaE(alternativaE);
        q.setCorreta(letraCorreta);
        q.setPublica(publica);
        q.setAutorId(prof.id()); // ProfSessao é record → id()

        try {
            questaoRepo.save(q);
            ra.addFlashAttribute("ok", "Questão cadastrada com sucesso!");
            return "redirect:/prof/questoes/minhas";
        } catch (Exception ex) {
            // Pode ser violação de unicidade (area_id, enunciado)
            ra.addFlashAttribute("erro", "Não foi possível salvar. Verifique se já não existe questão igual nesta área.");
            return "redirect:/prof/questoes/nova";
        }
    }

    // ========= editar =========

    /**
     * Formulário de edição (somente autor).
     * View: templates/prof/questoes/form.html (reutiliza; no template use 'q' para preencher os campos)
     */
    @GetMapping("/{id}/editar")
    public String formEditar(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes ra) {
        ProfSessao prof = profFrom(session);
        String redirect = requireLogin(prof);
        if (redirect != null) return redirect;

        Optional<Questao> opt = questaoRepo.findById(id);
        if (opt.isEmpty() || !opt.get().isAtivo() || !opt.get().getAutorId().equals(prof.id())) {
            ra.addFlashAttribute("erro", "Questão não encontrada ou não pertence a você.");
            return "redirect:/prof/questoes/minhas";
        }

        model.addAttribute("q", opt.get());
        model.addAttribute("areas", areaRepo.findAll());
        return "prof/questoes/form";
    }

    /**
     * Salva edição de questão (somente autor).
     */
    @PostMapping("/{id}/editar")
    public String salvarEditar(@PathVariable Long id,
                               @RequestParam Long areaId,
                               @RequestParam String enunciado,
                               @RequestParam String alternativaA,
                               @RequestParam String alternativaB,
                               @RequestParam String alternativaC,
                               @RequestParam String alternativaD,
                               @RequestParam(required = false) String alternativaE,
                               @RequestParam String correta,
                               @RequestParam(name = "publica", defaultValue = "false") boolean publica,
                               HttpSession session,
                               RedirectAttributes ra) {
        ProfSessao prof = profFrom(session);
        String redirect = requireLogin(prof);
        if (redirect != null) return redirect;

        Optional<Questao> opt = questaoRepo.findById(id);
        if (opt.isEmpty() || !opt.get().isAtivo() || !opt.get().getAutorId().equals(prof.id())) {
            ra.addFlashAttribute("erro", "Questão não encontrada ou não pertence a você.");
            return "redirect:/prof/questoes/minhas";
        }

        String erro = validar(enunciado, alternativaA, alternativaB, alternativaC, alternativaD, correta);
        if (erro != null) {
            ra.addFlashAttribute("erro", erro);
            return "redirect:/prof/questoes/" + id + "/editar";
        }

        Optional<AreaConhecimento> areaOpt = areaRepo.findById(areaId);
        if (areaOpt.isEmpty()) {
            ra.addFlashAttribute("erro", "Área inválida.");
            return "redirect:/prof/questoes/" + id + "/editar";
        }

        Questao q = opt.get();
        q.setAreaConhecimento(areaOpt.get());
        q.setEnunciado(enunciado);
        q.setAlternativaA(alternativaA);
        q.setAlternativaB(alternativaB);
        q.setAlternativaC(alternativaC);
        q.setAlternativaD(alternativaD);
        q.setAlternativaE(alternativaE);
        q.setPublica(publica);
        q.setCorreta(Character.toUpperCase(correta.trim().charAt(0)));

        try {
            questaoRepo.save(q);
            ra.addFlashAttribute("ok", "Questão atualizada com sucesso!");
            return "redirect:/prof/questoes/minhas";
        } catch (Exception ex) {
            ra.addFlashAttribute("erro", "Não foi possível atualizar. Verifique duplicidade do enunciado na mesma área.");
            return "redirect:/prof/questoes/" + id + "/editar";
        }
    }

    // ========= excluir =========

    /**
     * Exclui definitivamente a questão (somente autor).
     * Se preferir remoção lógica, troque por: q.setAtivo(false); questaoRepo.save(q);
     */
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        ProfSessao prof = profFrom(session);
        String redirect = requireLogin(prof);
        if (redirect != null) return redirect;

        Optional<Questao> opt = questaoRepo.findById(id);
        if (opt.isEmpty() || !opt.get().isAtivo() || !opt.get().getAutorId().equals(prof.id())) {
            ra.addFlashAttribute("erro", "Questão não encontrada ou não pertence a você.");
            return "redirect:/prof/questoes/minhas";
        }

        questaoRepo.delete(opt.get());
        ra.addFlashAttribute("ok", "Questão excluída.");
        return "redirect:/prof/questoes/minhas";
    }


    /**
     * Buscar elementos do repositorio
     */
    @GetMapping("/buscar")
    public Map<String,Object> buscar(@RequestParam(required=false) String q,
                                     @RequestParam(required=false) Long areaId,
                                     @RequestParam(defaultValue="true") boolean minhas,
                                     @RequestParam(defaultValue="true") boolean publicas,
                                     @RequestParam(defaultValue="0") int page,
                                     @RequestParam(defaultValue="10") int size,
                                     HttpSession session) {
        var sess = (ProfSessao) session.getAttribute("PROF_AUTH");
        if (sess == null) return Map.of("content", java.util.List.of(), "totalPages", 0, "number", 0);

        var pg = questaoRepo.buscarParaProva(
                sess.id(), minhas, publicas,
                areaId, (q == null || q.isBlank()) ? null : q.trim(),
                PageRequest.of(Math.max(page,0), Math.min(Math.max(size,1),50))
        );

        var content = pg.getContent().stream().map((Questao qq) -> Map.of(
                "id", qq.getId(),
                "area", (qq.getAreaConhecimento() != null ? qq.getAreaConhecimento().getNome() : "Sem área"),
                "publica", Boolean.TRUE.equals(qq.getPublica()), // << aqui
                "enunciado", qq.getEnunciado()
        )).toList();

        return Map.of("content", content, "totalPages", pg.getTotalPages(), "number", pg.getNumber());
    }
}
