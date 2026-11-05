package br.com.plataforma.contratos.documento;

import java.util.UUID;

/**
 * Onde os bytes do arquivo ficam. O resto do módulo só conhece esta interface: hoje a implementação
 * é o PostgreSQL (ArmazenamentoNoBanco); trocar por disco ou MinIO é escrever outra classe, sem mexer nos endpoints.
 * O registro (metadados) do documento já existe quando estes métodos são chamados.
 */
public interface ArmazenamentoDeArquivos {

    void salvar(UUID documentoId, byte[] conteudo);

    byte[] ler(UUID documentoId);

    void excluir(UUID documentoId);
}
