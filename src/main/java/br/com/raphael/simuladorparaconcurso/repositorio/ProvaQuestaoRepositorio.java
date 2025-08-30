package br.com.raphael.simuladorparaconcurso.repositorio;

import br.com.raphael.simuladorparaconcurso.dominio.ProvaQuestao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProvaQuestaoRepositorio extends JpaRepository<ProvaQuestao, Long> {
    List<ProvaQuestao> findByProvaIdOrderByOrdemAsc(Long provaId);
    void deleteByProvaId(Long provaId);
}
