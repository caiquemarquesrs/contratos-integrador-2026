package br.com.plataforma.contratos.crm;

import java.util.Optional;
import java.util.UUID;

/**
 * Consulta ao módulo CRM. Existe como interface para o teste trocar o CRM de verdade por um falso:
 * o CI não tem CRM no ar (Contrato de Integração §14.5).
 */
public interface ClienteCrm {

    /**
     * Razão social da empresa, como o CRM a conhece agora. Devolve vazio se o CRM não respondeu
     * ou respondeu algo inútil: este método nunca lança exceção, porque a falha de outro módulo
     * não pode derrubar a operação (Contrato §9.6).
     *
     * @param tokenDoUsuario o mesmo token recebido na requisição, repassado como está (§9.2)
     */
    Optional<String> razaoSocial(UUID empresaId, String tokenDoUsuario);
}
