package br.com.plataforma.contratos.api;

/**
 * Valor inválido que só o serviço consegue conferir (por exemplo, um modeloId que não existe na
 * empresa): responde 400 com o campo no envelope, igual a um erro de validação do corpo.
 */
public class CampoInvalidoException extends RuntimeException {

    private final transient ErroCampo erro;

    public CampoInvalidoException(String campo, String codigo, String detalhe) {
        super(detalhe);
        this.erro = new ErroCampo(campo, codigo, detalhe);
    }

    public ErroCampo getErro() {
        return erro;
    }
}
