package br.com.raphael.simuladorparaconcurso.repositorio;

import br.com.raphael.simuladorparaconcurso.dominio.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessorRepositorio extends JpaRepository<Professor, Long> {
    Optional<Professor> findByEmailIgnoreCaseAndAtivoTrue(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
