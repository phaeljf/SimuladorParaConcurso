package br.com.raphael.simuladorparaconcurso.web.dto;

import br.com.raphael.simuladorparaconcurso.dominio.Prova;

public record ProvaResumoDTO(
        Long id,
        String titulo,
        String professorNome
) {
    public static ProvaResumoDTO of(Prova p) {
        return new ProvaResumoDTO(
                p.getId(),
                p.getTitulo(),
                p.getCriadoPor() != null ? p.getCriadoPor().getNome() : ""
        );
    }
}
