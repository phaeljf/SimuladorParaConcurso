package br.com.raphael.simuladorparaconcurso.web;

import br.com.raphael.simuladorparaconcurso.dominio.Professor;
import br.com.raphael.simuladorparaconcurso.modelo.ProfSessao;
import br.com.raphael.simuladorparaconcurso.repositorio.ProfessorRepositorio;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/prof/perfil")
public class ProfPerfilController {

    private final ProfessorRepositorio professorRepo;

    private ProfSessao profFrom(HttpSession s) {
        return (ProfSessao) s.getAttribute("PROF_AUTH");
    }

    @GetMapping
    public String ver(Model model, HttpSession session) {
        var prof = profFrom(session);
        if (prof == null) return "redirect:/prof/login";
        Professor p = professorRepo.findById(prof.id()).orElseThrow();
        model.addAttribute("p", p);
        return "prof/perfil/prof-form";
    }

    @PostMapping("/salvar")
    public String salvar(@RequestParam String nome,
                         @RequestParam String email,
                         @RequestParam(required=false) String profissao,
                         @RequestParam(required=false) String telefone,
                         HttpSession session,
                         RedirectAttributes ra) {

        var sess = profFrom(session);
        if (sess == null) return "redirect:/prof/login";

        // Evitar duplicidade de e-mail
        var existe = professorRepo.existsByEmailIgnoreCaseAndIdNot(email, sess.id());
        if (existe) {
            ra.addFlashAttribute("erro", "E-mail já em uso por outro professor.");
            return "redirect:/prof/perfil";
        }


        Professor p = professorRepo.findById(sess.id()).orElseThrow();
        p.setNome(nome);
        p.setEmail(email);
        p.setProfissao(profissao);
        p.setTelefone(telefone);
        professorRepo.save(p);

        // Atualiza a sessão se o e-mail/nome mudou
        session.setAttribute("PROF_AUTH", new br.com.raphael.simuladorparaconcurso.modelo.ProfSessao(
                p.getId(), p.getNome(), p.getEmail()
        ));

        ra.addFlashAttribute("ok", "Perfil atualizado com sucesso.");
        return "redirect:/prof/perfil";
    }

    @PostMapping("/senha")
    public String trocarSenha(@RequestParam String senhaAtual,
                              @RequestParam String novaSenha,
                              @RequestParam String confirmaSenha,
                              HttpSession session,
                              RedirectAttributes ra) {
        var sess = profFrom(session);
        if (sess == null) return "redirect:/prof/login";

        if (!novaSenha.equals(confirmaSenha)) {
            ra.addFlashAttribute("erroSenha", "Confirmação de senha não confere.");
            return "redirect:/prof/perfil";
        }

        Professor p = professorRepo.findById(sess.id()).orElseThrow();
        if (!BCrypt.checkpw(senhaAtual, p.getSenhaHash())) {
            ra.addFlashAttribute("erroSenha", "Senha atual incorreta.");
            return "redirect:/prof/perfil";
        }

        String hash = BCrypt.hashpw(novaSenha, BCrypt.gensalt());
        p.setSenhaHash(hash);
        professorRepo.save(p);

        ra.addFlashAttribute("okSenha", "Senha alterada.");
        return "redirect:/prof/perfil";
    }
}
