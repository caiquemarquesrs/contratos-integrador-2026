package br.com.plataforma.contratos.documento;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;

/**
 * Guarda o arquivo na coluna bytea da própria linha do documento. Usa o EntityManager para
 * participar da mesma transação do cadastro: se algo falhar depois, o arquivo some junto com o registro.
 */
@Component
public class ArmazenamentoNoBanco implements ArmazenamentoDeArquivos {

    private final EntityManager em;

    public ArmazenamentoNoBanco(EntityManager em) {
        this.em = em;
    }

    @Override
    public void salvar(UUID documentoId, byte[] conteudo) {
        int linhas = em.createNativeQuery("update contratos.documentos set conteudo = :conteudo where id = :id")
                .setParameter("conteudo", conteudo)
                .setParameter("id", documentoId)
                .executeUpdate();
        if (linhas != 1) {
            throw new IllegalStateException("Não foi possível gravar o arquivo do documento " + documentoId + ".");
        }
    }

    @Override
    public byte[] ler(UUID documentoId) {
        List<?> resultado = em.createNativeQuery(
                        "select conteudo from contratos.documentos where id = :id and deleted_at is null")
                .setParameter("id", documentoId)
                .getResultList();
        if (resultado.isEmpty() || resultado.get(0) == null) {
            throw new IllegalStateException("O arquivo do documento " + documentoId + " não está guardado.");
        }
        return (byte[]) resultado.get(0);
    }

    @Override
    public void excluir(UUID documentoId) {
        em.createNativeQuery("update contratos.documentos set conteudo = null where id = :id")
                .setParameter("id", documentoId)
                .executeUpdate();
    }
}
