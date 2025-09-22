package br.com.raphael.simuladorparaconcurso.web.dto;

import br.com.raphael.simuladorparaconcurso.dominio.Prova;

public record ProvaPesquisaDTO(
        Long id,
        String titulo,
        String professorNome,
        Integer tempoMinutos,
        boolean publica,
        boolean mostrarGabarito
) {
    public static ProvaPesquisaDTO of(Prova p) {
        return new ProvaPesquisaDTO(
                p.getId(),
                p.getTitulo(),
                p.getCriadoPor() != null ? p.getCriadoPor().getNome() : "",
                p.getTempoMinutos(),
                // Se na sua entidade os getters são getPublica()/getMostrarGabarito(), ajuste abaixo:
                p.getPublica(),
                p.getMostrarGabarito()
        );
    }
}
