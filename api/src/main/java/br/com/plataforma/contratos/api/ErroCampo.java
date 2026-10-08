package br.com.plataforma.contratos.api;

/** Detalhe de um erro, campo a campo, dentro do envelope. */
public record ErroCampo(String campo, String codigo, String detalhe) {
}
