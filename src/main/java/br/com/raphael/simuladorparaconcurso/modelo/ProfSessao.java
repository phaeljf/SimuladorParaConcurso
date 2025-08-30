package br.com.raphael.simuladorparaconcurso.modelo;

import java.io.Serializable;

public record ProfSessao(Long id, String nome, String email) implements Serializable {}
