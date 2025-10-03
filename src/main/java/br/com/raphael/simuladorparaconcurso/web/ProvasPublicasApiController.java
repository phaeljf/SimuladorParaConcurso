package br.com.raphael.simuladorparaconcurso.web;
import br.com.raphael.simuladorparaconcurso.web.dto.ProvaResumoDTO;
import br.com.raphael.simuladorparaconcurso.web.dto.ProvaPesquisaDTO;
import br.com.raphael.simuladorparaconcurso.repositorio.ProvaRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provas")
@RequiredArgsConstructor
public class ProvasPublicasApiController {

    private final ProvaRepositorio provaRepo;

    // /api/provas/search?q=log
    @GetMapping("/search")
    public List<ProvaResumoDTO> search(@RequestParam(required = false) String q) {
        var limit = PageRequest.of(0, 6); // limita as sugestões do index
        return provaRepo.buscarPublicasPorNomeOuProfessor(q, limit)
                .stream().map(ProvaResumoDTO::of).toList();
    }

    // /api/provas/advanced?titulo=...&prof=...&area=...
    @GetMapping("/advanced")
    public List<ProvaPesquisaDTO> advanced(@RequestParam(required = false) String titulo,
                                           @RequestParam(required = false, name = "prof") String professor,
                                           @RequestParam(required = false) String area) {
        return provaRepo.pesquisaPublicaAvancada(titulo, professor, area)
                .stream().map(ProvaPesquisaDTO::of).toList();
    }
}
