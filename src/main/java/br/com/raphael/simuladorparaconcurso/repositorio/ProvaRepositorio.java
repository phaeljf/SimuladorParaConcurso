package br.com.raphael.simuladorparaconcurso.repositorio;

import br.com.raphael.simuladorparaconcurso.dominio.Prova;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProvaRepositorio extends JpaRepository<Prova, Long> {

    Page<Prova> findByCriadoPorIdAndAtivoTrueOrderByCriadoEmDesc(Long profId, Pageable pageable);

    Page<Prova> findByCriadoPorIdAndAtivoTrueAndTituloContainingIgnoreCaseOrderByCriadoEmDesc(
            Long profId, String titulo, Pageable pageable);

    Optional<Prova> findByIdAndCriadoPorIdAndAtivoTrue(Long id, Long profId);

    // Busca rápida para o index (por título)
    @Query("""
       select p
       from Prova p
       join p.criadoPor pr
       where p.publica = true
         and p.ativo   = true
         and (
              :q is null or :q = ''
              or lower(p.titulo) like lower(concat('%', :q, '%'))
              or lower(pr.nome)  like lower(concat('%', :q, '%'))
         )
       order by p.titulo asc
       """)
    List<Prova> buscarPublicasPorNomeOuProfessor(@Param("q") String q, Pageable pageable);

    // Pesquisa avançada (título, professor, área)
    @Query("""
              select distinct p
              from Prova p
              left join p.criadoPor pr
              left join ProvaQuestao pq on pq.prova = p
              left join pq.questao q
              left join q.areaConhecimento a
              where p.publica = true
                and p.ativo = true
                and (:titulo is null or :titulo = '' or lower(p.titulo) like lower(concat('%', :titulo, '%')))
                and (:prof   is null or :prof   = '' or lower(pr.nome)   like lower(concat('%', :prof,   '%')))
                and (:area   is null or :area   = '' or lower(a.nome)    like lower(concat('%', :area,   '%')))
              order by p.titulo asc
            """)
    List<Prova> pesquisaPublicaAvancada(@Param("titulo") String titulo,
                                        @Param("prof")   String professor,
                                        @Param("area")   String area);

}
