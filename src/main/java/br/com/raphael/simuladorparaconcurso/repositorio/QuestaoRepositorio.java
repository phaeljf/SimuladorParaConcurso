package br.com.raphael.simuladorparaconcurso.repositorio;

import br.com.raphael.simuladorparaconcurso.dominio.Questao;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface QuestaoRepositorio extends JpaRepository<Questao, Long> {

    @Query(value = """
        SELECT * FROM questoes
         WHERE area_id = :areaId AND ativo = true
         ORDER BY random()
         LIMIT :n
    """, nativeQuery = true)
    List<Questao> buscarAleatoriasPorArea(@Param("areaId") Long areaId, @Param("n") int n);

    int countByAreaConhecimentoIdAndAtivoTrue(Long areaId);

    // QuestaoRepositorio.java
    List<Questao> findTop50ByAutorIdAndAtivoTrueOrderByIdDesc(Long autorId);

    // Carregar garantindo autoria (para editar/excluir com segurança)
    Optional<Questao> findByIdAndAutorIdAndAtivoTrue(Long id, Long autorId);

    // Excluir garantindo autoria (retorna linhas afetadas)
    long deleteByIdAndAutorId(Long id, Long autorId);

    // Listar/paginar “minhas”
    Page<Questao> findByAutorIdAndAtivoTrue(Long autorId, Pageable pageable);

    // Filtro por área dentro de “minhas”
    Page<Questao> findByAutorIdAndAtivoTrueAndAreaConhecimentoId(Long autorId, Long areaId, Pageable pageable);

    // Busca textual simples (enunciado) – “minhas”
    Page<Questao> findByAutorIdAndAtivoTrueAndEnunciadoContainingIgnoreCase(Long autorId, String termo, Pageable pageable);

    // Busca pública (para pesquisa geral ou montar prova pública)
    Page<Questao> findByPublicaTrueAndAtivoTrue(Pageable pageable);
    Page<Questao> findByPublicaTrueAndAtivoTrueAndEnunciadoContainingIgnoreCase(String termo, Pageable pageable);


    // paginação "minhas"
    Page<Questao> findByAutorIdAndAtivoTrueOrderByIdDesc(Long autorId, Pageable pageable);

    // variações simples (usaremos de forma combinada)
    Page<Questao> findByAutorIdAndAtivoTrueAndAreaConhecimentoIdOrderByIdDesc(Long autorId, Long areaId, Pageable pageable);
    Page<Questao> findByAutorIdAndAtivoTrueAndEnunciadoContainingIgnoreCaseOrderByIdDesc(Long autorId, String q, Pageable pageable);
    Page<Questao> findByAutorIdAndAtivoTrueAndAreaConhecimentoIdAndEnunciadoContainingIgnoreCaseOrderByIdDesc(Long autorId, Long areaId, String q, Pageable pageable);

    // busca para o popup de Prova (minhas/publicas/ambas + filtros)
    @Query(
            value = """
  select q.* 
  from questoes q
  where q.ativo = true
    and ((:minhas = true and q.autor_id = :profId) or (:publicas = true and q.publica = true))
    and (:areaId is null or q.area_id = :areaId)
    and (:texto is null or CAST(q.enunciado AS text) ilike concat('%', :texto, '%'))
  order by q.id desc
  """,
            countQuery = """
  select count(*) 
  from questoes q
  where q.ativo = true
    and ((:minhas = true and q.autor_id = :profId) or (:publicas = true and q.publica = true))
    and (:areaId is null or q.area_id = :areaId)
    and (:texto is null or CAST(q.enunciado AS text) ilike concat('%', :texto, '%'))
  """,
            nativeQuery = true
    )
    Page<Questao> buscarParaProva(@Param("profId") Long profId,
                                  @Param("minhas") boolean minhas,
                                  @Param("publicas") boolean publicas,
                                  @Param("areaId") Long areaId,
                                  @Param("texto") String texto,
                                  Pageable pageable);

}
