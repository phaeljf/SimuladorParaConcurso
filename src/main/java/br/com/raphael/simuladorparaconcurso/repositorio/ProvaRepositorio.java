package br.com.raphael.simuladorparaconcurso.repositorio;

import br.com.raphael.simuladorparaconcurso.dominio.Prova;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProvaRepositorio extends JpaRepository<Prova, Long> {
    List<Prova> findByCriadoPorIdOrderByCriadoEmDesc(Long profId);
    Optional<Prova> findByIdAndCriadoPorId(Long id, Long profId);

    Page<Prova> findByCriadoPorIdOrderByCriadoEmDesc(Long profId, Pageable pageable);
    Page<Prova> findByCriadoPorIdAndTituloContainingIgnoreCaseOrderByCriadoEmDesc(Long profId, String titulo, Pageable pageable);

    List<Prova> findByCriadoPorIdAndAtivoTrueOrderByCriadoEmDesc(Long profId);

    Page<Prova> findByCriadoPorIdAndAtivoTrueOrderByCriadoEmDesc(Long profId, Pageable pageable);

    Page<Prova> findByCriadoPorIdAndAtivoTrueAndTituloContainingIgnoreCaseOrderByCriadoEmDesc(
            Long profId, String titulo, Pageable pageable);

    Optional<Prova> findByIdAndCriadoPorIdAndAtivoTrue(Long id, Long profId);
}
