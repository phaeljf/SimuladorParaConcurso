package br.com.raphael.simuladorparaconcurso.repositorio;

import br.com.raphael.simuladorparaconcurso.dominio.ProvaQuestao;
import br.com.raphael.simuladorparaconcurso.web.dto.AreaQtdDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


public interface ProvaQuestaoRepositorio extends JpaRepository<ProvaQuestao, Long> {
    List<ProvaQuestao> findByProvaId(Long provaId);
    List<ProvaQuestao> findByProvaIdOrderByOrdemAsc(Long provaId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("delete from ProvaQuestao pq where pq.prova.id = :provaId")
    void deleteByProvaId(@Param("provaId") Long provaId);

    // (opcional) pode ser útil no futuro:
    boolean existsByProvaIdAndQuestaoId(Long provaId, Long questaoId);

    @Query("""
       select new br.com.raphael.simuladorparaconcurso.web.dto.AreaQtdDTO(
                q.areaConhecimento.id,
                q.areaConhecimento.nome,
                q.areaConhecimento.descricao,
                count(pq)
       )
       from ProvaQuestao pq
         join pq.questao q
       where pq.prova.id = :provaId
       group by q.areaConhecimento.id, q.areaConhecimento.nome, q.areaConhecimento.descricao
       order by q.areaConhecimento.nome
       """)
    List<AreaQtdDTO> contagemPorArea(@Param("provaId") Long provaId);



}
