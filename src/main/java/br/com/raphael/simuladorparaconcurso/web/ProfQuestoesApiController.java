package br.com.raphael.simuladorparaconcurso.web;

import br.com.raphael.simuladorparaconcurso.dominio.Questao;
import br.com.raphael.simuladorparaconcurso.modelo.ProfSessao;
import br.com.raphael.simuladorparaconcurso.repositorio.QuestaoRepositorio;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/prof/questoes/api")
public class ProfQuestoesApiController {

    private final QuestaoRepositorio questaoRepo;

    @GetMapping("/buscar")
    public Map<String,Object> buscar(@RequestParam(required=false) String q,
                                     @RequestParam(required=false, name="areaId") String areaIdStr,
                                     @RequestParam(defaultValue="true") boolean minhas,
                                     @RequestParam(defaultValue="true") boolean publicas,
                                     @RequestParam(defaultValue="0") int page,
                                     @RequestParam(defaultValue="10") int size,
                                     HttpSession session) {
        var sess = (ProfSessao) session.getAttribute("PROF_AUTH");
        if (sess == null) return Map.of("content", java.util.List.of(), "totalPages", 0, "number", 0);

        Long areaId = null;
        if (areaIdStr != null && !areaIdStr.isBlank()) {
            try { areaId = Long.valueOf(areaIdStr.trim()); } catch (NumberFormatException ignore) { areaId = null; }
        }

        var texto = (q == null || q.isBlank()) ? null : q.trim();

        var pg = questaoRepo.buscarParaProva(
                sess.id(), minhas, publicas, areaId, texto,
                org.springframework.data.domain.PageRequest.of(Math.max(page,0), Math.min(Math.max(size,1),50))
        );

        var content = pg.getContent().stream().map((Questao qq) -> Map.of(
                "id", qq.getId(),
                "area", (qq.getAreaConhecimento() != null ? qq.getAreaConhecimento().getNome() : "Sem área"),
                "publica", Boolean.TRUE.equals(qq.getPublica()),
                "enunciado", qq.getEnunciado()
        )).toList();

        return Map.of("content", content, "totalPages", pg.getTotalPages(), "number", pg.getNumber());
    }

}
