package br.com.plataforma.contratos.integracao;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.plataforma.contratos.api.Resposta;

/**
 * Leitura para outros módulos: busca global, resumo de contrato e status contratual de empresa.
 * Todos exigem a permissão fraca contratos.contrato.ver_resumo, que o token de serviço do
 * Financeiro também recebe (Contrato §9.3). Esta classe e a ContratoController dividem o prefixo
 * /api/contratos/contratos: em GET /contratos/resumo o Spring escolhe o caminho fixo, não o /{id}.
 */
@RestController
@RequestMapping("/api/contratos")
public class IntegracaoController {

    private static final String VER_RESUMO = "hasAuthority('contratos.contrato.ver_resumo')";

    private final IntegracaoServico servico;

    public IntegracaoController(IntegracaoServico servico) {
        this.servico = servico;
    }

    /** q ausente vale o mesmo que q curto: 400 com campo "q" (e não o erro genérico de parâmetro faltando). */
    @GetMapping("/busca")
    @PreAuthorize(VER_RESUMO)
    public Resposta<List<ContratoBusca>> buscar(@RequestParam(name = "q", defaultValue = "") String q) {
        return Resposta.ok(servico.buscar(q));
    }

    @GetMapping("/contratos/{id}/resumo")
    @PreAuthorize(VER_RESUMO)
    public Resposta<ContratoResumo> resumo(@PathVariable UUID id) {
        return Resposta.ok(servico.resumo(id));
    }

    @GetMapping("/contratos/resumo")
    @PreAuthorize(VER_RESUMO)
    public Resposta<List<ContratoResumo>> resumos(@RequestParam List<UUID> ids) {
        return Resposta.ok(servico.resumos(ids));
    }

    @GetMapping("/empresas/{empresaId}/status-contratual")
    @PreAuthorize(VER_RESUMO)
    public Resposta<StatusContratual> statusContratual(@PathVariable UUID empresaId) {
        return Resposta.ok(servico.statusContratual(empresaId));
    }

    @GetMapping("/empresas/status-contratual")
    @PreAuthorize(VER_RESUMO)
    public Resposta<List<StatusContratual>> statusContratuais(@RequestParam List<UUID> ids) {
        return Resposta.ok(servico.statusContratuais(ids));
    }
}
