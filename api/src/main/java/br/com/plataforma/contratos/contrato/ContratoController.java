package br.com.plataforma.contratos.contrato;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.plataforma.contratos.api.Pagina;
import br.com.plataforma.contratos.api.Resposta;
import br.com.plataforma.contratos.crm.ClienteCrm;
import br.com.plataforma.contratos.seguranca.Usuarios;
import jakarta.validation.Valid;

/**
 * Contratos (RF-01). Toda operação verifica permissão no back-end (Contrato §5.2): esconder botão
 * no front é usabilidade; quem chamar a API sem a permissão recebe 403.
 */
@RestController
@RequestMapping("/api/contratos/contratos")
public class ContratoController {

    private final ContratoServico servico;
    private final ClienteCrm crm;

    public ContratoController(ContratoServico servico, ClienteCrm crm) {
        this.servico = servico;
        this.crm = crm;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('contratos.contrato.ver')")
    public Resposta<Pagina<ContratoDto>> listar(@RequestParam(defaultValue = "0") int pagina,
                                                @RequestParam(defaultValue = "20") int tamanho,
                                                @RequestParam(required = false) String q,
                                                @RequestParam(required = false) UUID empresaId,
                                                @RequestParam(required = false) StatusContrato status,
                                                @RequestParam(defaultValue = "criadoEm,desc") String ordenar) {
        return Resposta.ok(servico.listar(q, empresaId, status, ordenar,
                Math.max(pagina, 0), Math.clamp(tamanho, 1, 100)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('contratos.contrato.ver')")
    public Resposta<ContratoDto> ver(@PathVariable UUID id) {
        return Resposta.ok(servico.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('contratos.contrato.criar')")
    public ResponseEntity<Resposta<ContratoDto>> criar(@Valid @RequestBody NovoContrato corpo,
                                                       @AuthenticationPrincipal Jwt jwt) {
        String empresaNome = crm.razaoSocial(corpo.empresaId(), jwt.getTokenValue()).orElse(null);
        ContratoDto contrato = servico.criar(corpo, Usuarios.idDe(jwt), empresaNome);
        return ResponseEntity.created(URI.create("/api/contratos/contratos/" + contrato.id()))
                .body(Resposta.ok(contrato));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('contratos.contrato.editar')")
    public Resposta<ContratoDto> editar(@PathVariable UUID id, @Valid @RequestBody EdicaoContrato corpo,
                                        @AuthenticationPrincipal Jwt jwt) {
        return Resposta.ok(servico.editar(id, corpo, Usuarios.idDe(jwt)));
    }
}
