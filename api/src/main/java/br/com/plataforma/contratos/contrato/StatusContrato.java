package br.com.plataforma.contratos.contrato;

import java.util.Set;

/** Os nove estados do contrato, com os mesmos nomes do contratos.yaml. */
public enum StatusContrato {
    RASCUNHO,
    EM_APROVACAO,
    AGUARDANDO_ASSINATURA,
    ATIVO,
    A_VENCER,
    RENOVADO,
    SUSPENSO,
    CANCELADO,
    FINALIZADO;

    private static final Set<StatusContrato> EDITAVEIS = Set.of(RASCUNHO, EM_APROVACAO);

    /** Só rascunho e em aprovação aceitam edição (PUT); nos demais estados a resposta é 409. */
    public boolean permiteEdicao() {
        return EDITAVEIS.contains(this);
    }
}
