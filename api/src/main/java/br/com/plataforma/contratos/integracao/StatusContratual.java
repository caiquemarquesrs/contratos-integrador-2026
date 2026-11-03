package br.com.plataforma.contratos.integracao;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonValue;

/** Schema StatusContratual do contratos.yaml: a situação contratual de uma empresa. */
public record StatusContratual(
        UUID empresaId,
        Situacao situacao,
        UUID contratoId,         // contrato que determinou a situação; nulo em sem_contrato
        String numero,           // número desse contrato; nulo em sem_contrato
        Integer diasParaVencer) {   // nulo sem contrato ou sem vigenciaFim; 0 se já venceu

    /** Da menos para a mais grave (a ordem das constantes é a gravidade). No JSON vai o texto do contratos.yaml. */
    public enum Situacao {
        SEM_CONTRATO("sem_contrato"),
        OK("ok"),
        ATENCAO("atencao"),
        CRITICO("critico");

        private final String valor;

        Situacao(String valor) {
            this.valor = valor;
        }

        @JsonValue
        public String valor() {
            return valor;
        }
    }

    static StatusContratual semContrato(UUID empresaId) {
        return new StatusContratual(empresaId, Situacao.SEM_CONTRATO, null, null, null);
    }
}
