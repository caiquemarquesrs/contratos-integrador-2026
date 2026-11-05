package br.com.plataforma.contratos.documento;

/** Tamanho máximo de um documento. O contrato não define o valor: é decisão do grupo (5 MB). */
public final class LimiteDeUpload {

    public static final long MAXIMO_EM_BYTES = 5L * 1024 * 1024;

    private LimiteDeUpload() {
    }
}
