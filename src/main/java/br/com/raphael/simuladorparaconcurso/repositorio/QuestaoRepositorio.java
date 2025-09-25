package br.com.raphael.simuladorparaconcurso.repositorio;
import br.com.raphael.simuladorparaconcurso.dominio.Questao;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface QuestaoRepositorio extends JpaRepository<Questao, Long> {

    /* ==== ESSENCIAIS PARA “MINHAS QUESTÕES” (CRUD seguro) ==== */

    // Listagem do professor (paginada, mais usada no dia a dia)
    Page<Questao> findByAutorIdAndAtivoTrueOrderByIdDesc(Long autorId, Pageable pageable);

    // (Opcional) contagem por área (alguns fluxos usam)
    int countByAreaConhecimentoIdAndAtivoTrue(Long areaId);


    /* ==== SORTEIO (para montar prova/simulado) ==== */

    // Sorteio simples por área (sem filtros)
    @Query(value = """
        SELECT * FROM questoes
         WHERE ativo = true AND publica = true
           AND area_id = :areaId
         ORDER BY random()
         LIMIT :n
    """, nativeQuery = true)
    List<Questao> buscarAleatoriasPorArea(@Param("areaId") Long areaId,
                                          @Param("n") int n);

    // Sorteio por área + filtros (qualquer um pode ser nulo → ignora)
    @Query(value = """
        SELECT * FROM questoes
         WHERE ativo = true AND publica = true
           AND area_id = :areaId
           AND (:dificuldade IS NULL OR UPPER(dificuldade)  = UPPER(CAST(:dificuldade  AS varchar)))
           AND (:escolaridade IS NULL OR UPPER(escolaridade) = UPPER(CAST(:escolaridade AS varchar)))
         ORDER BY random()
         LIMIT :n
    """, nativeQuery = true)
    List<Questao> buscarAleatoriasPorAreaComFiltros(@Param("areaId") Long areaId,
                                                    @Param("n") int n,
                                                    @Param("dificuldade") String dificuldade,
                                                    @Param("escolaridade") String escolaridade);

    /* ==== POPUP DE BUSCA PARA PROVA (minhas/publicas + filtros) ==== */

    @Query(
            value = """
            SELECT q.*
              FROM questoes q
             WHERE q.ativo = true
               AND ((:minhas = true  AND q.autor_id = :profId)
                 OR (:publicas = true AND q.publica  = true))
               AND (:areaId IS NULL OR q.area_id = :areaId)
               AND (:texto  IS NULL OR CAST(q.enunciado AS text) ILIKE CONCAT('%%', :texto, '%%'))
               AND (:dificuldade IS NULL OR UPPER(q.dificuldade)  = UPPER(CAST(:dificuldade  AS varchar)))
               AND (:escolaridade IS NULL OR UPPER(q.escolaridade) = UPPER(CAST(:escolaridade AS varchar)))
             ORDER BY q.id DESC
        """,
            countQuery = """
            SELECT COUNT(*)
              FROM questoes q
             WHERE q.ativo = true
               AND ((:minhas = true  AND q.autor_id = :profId)
                 OR (:publicas = true AND q.publica  = true))
               AND (:areaId IS NULL OR q.area_id = :areaId)
               AND (:texto  IS NULL OR CAST(q.enunciado AS text) ILIKE CONCAT('%%', :texto, '%%'))
               AND (:dificuldade IS NULL OR UPPER(q.dificuldade)  = UPPER(CAST(:dificuldade  AS varchar)))
               AND (:escolaridade IS NULL OR UPPER(q.escolaridade) = UPPER(CAST(:escolaridade AS varchar)))
        """,
            nativeQuery = true
    )
    Page<Questao> buscarParaProva(@Param("profId") Long profId,
                                  @Param("minhas") boolean minhas,
                                  @Param("publicas") boolean publicas,
                                  @Param("areaId") Long areaId,
                                  @Param("texto") String texto,
                                  @Param("dificuldade") String dificuldade,     // passe dif != null ? dif.name() : null
                                  @Param("escolaridade") String escolaridade,   // passe esc != null ? esc.name() : null
                                  Pageable pageable);

    // QuestaoRepositorio.java
    default org.springframework.data.domain.Page<br.com.raphael.simuladorparaconcurso.dominio.Questao>
    buscarParaProva(Long profId, boolean minhas, boolean publicas,
                    Long areaId, String texto,
                    org.springframework.data.domain.Pageable pageable) {
        return buscarParaProva(profId, minhas, publicas, areaId, texto, null, null, pageable);
    }

    /* ==== FILTROS SIMPLES (úteis em telas) – OPCIONAIS ==== */

    Page<Questao> findByAutorIdAndAtivoTrueAndAreaConhecimentoIdOrderByIdDesc(Long autorId, Long areaId, Pageable pageable);

    Page<Questao> findByAutorIdAndAtivoTrueAndEnunciadoContainingIgnoreCaseOrderByIdDesc(Long autorId, String q, Pageable pageable);

    Page<Questao> findByAutorIdAndAtivoTrueAndAreaConhecimentoIdAndEnunciadoContainingIgnoreCaseOrderByIdDesc(
            Long autorId, Long areaId, String q, Pageable pageable);
}
