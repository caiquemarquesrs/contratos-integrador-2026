package br.com.plataforma.contratos.api;

/**
 * A operação não é permitida no estado atual do recurso: responde 409 (Contrato §8.4).
 * Traz campo, código e detalhe para montar o item de {@code errors}, como no exemplo
 * ConflitoDeEstado do contratos.yaml. Também será usada por aprovar e assinar (Tarefa 3).
 */
public class ConflitoDeEstadoException extends RuntimeException {

    private final transient ErroCampo erro;

    public ConflitoDeEstadoException(String mensagem, String campo, String codigo, String detalhe) {
        super(mensagem);
        this.erro = new ErroCampo(campo, codigo, detalhe);
    }

    public ErroCampo getErro() {
        return erro;
    }
}
