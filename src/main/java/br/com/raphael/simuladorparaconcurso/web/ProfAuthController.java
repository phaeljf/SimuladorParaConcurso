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

@Controller
@RequiredArgsConstructor
public class ProfAuthController {

    private final ProfessorRepositorio professorRepo;

    @GetMapping("/prof/login")
    public String formLogin() {
        return "prof/login";
    }

    @PostMapping("/prof/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String senha,
                          HttpSession session,
                          Model model) {

        Professor p = professorRepo.findByEmailIgnoreCaseAndAtivoTrue(email).orElse(null);
        if (p == null || !BCrypt.checkpw(senha, p.getSenhaHash())) {
            model.addAttribute("erro", "Credenciais inválidas");
            model.addAttribute("email", email);
            return "prof/login";
        }

        session.setAttribute("PROF_AUTH", new ProfSessao(p.getId(), p.getNome(), p.getEmail()));
        return "redirect:/prof";
    }

    @PostMapping("/prof/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/prof")
    public String painel(HttpSession session, Model model) {
        ProfSessao prof = (ProfSessao) session.getAttribute("PROF_AUTH");
        if (prof == null) return "redirect:/prof/login";
        model.addAttribute("prof", prof);
        return "prof/index";
    }
}
