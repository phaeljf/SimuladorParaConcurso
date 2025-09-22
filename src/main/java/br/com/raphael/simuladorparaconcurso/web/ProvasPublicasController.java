package br.com.raphael.simuladorparaconcurso.web;

import br.com.raphael.simuladorparaconcurso.repositorio.ProvaQuestaoRepositorio;
import br.com.raphael.simuladorparaconcurso.repositorio.ProvaRepositorio;
import br.com.raphael.simuladorparaconcurso.servico.SimuladoServico;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/provas")
public class ProvasPublicasController {

    private final ProvaRepositorio provaRepo;
    private final ProvaQuestaoRepositorio pqRepo;
    private final SimuladoServico simuladoServico;

    // ... seu GET /provas/{id} (detalhe) fica como está ...

}

