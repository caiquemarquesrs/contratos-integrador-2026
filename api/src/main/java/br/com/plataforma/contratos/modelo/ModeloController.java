package br.com.plataforma.contratos.modelo;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.plataforma.contratos.api.Pagina;
import br.com.plataforma.contratos.api.Resposta;
import br.com.plataforma.contratos.seguranca.Usuarios;
import jakarta.validation.Valid;

/**
 * Modelos de contrato (RF-03). Toda operação verifica permissão no back-end (Contrato §5.2):
 * esconder botão no front é usabilidade; quem chamar a API sem a permissão recebe 403.
 */
@RestController
@RequestMapping("/api/contratos/modelos")
public class ModeloController {

    private final ModeloServico servico;

    public ModeloController(ModeloServico servico) {
        this.servico = servico;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('contratos.modelo.ver')")
    public Resposta<Pagina<ModeloDto>> listar(@RequestParam(defaultValue = "0") int pagina,
                                              @RequestParam(defaultValue = "20") int tamanho) {
        PageRequest pedido = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamanho, 1, 100),
                Sort.by(Sort.Direction.ASC, "nome"));
        return Resposta.ok(Pagina.de(servico.listar(pedido), ModeloDto::de));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('contratos.modelo.ver')")
    public Resposta<ModeloDto> ver(@PathVariable UUID id) {
        return Resposta.ok(ModeloDto.de(servico.buscar(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('contratos.modelo.administrar')")
    public ResponseEntity<Resposta<ModeloDto>> criar(@Valid @RequestBody NovoModelo corpo,
                                                     @AuthenticationPrincipal Jwt jwt) {
        Modelo modelo = servico.criar(corpo.nome(), corpo.descricao(), corpo.conteudo(), Usuarios.idDe(jwt));
        return ResponseEntity.created(URI.create("/api/contratos/modelos/" + modelo.getId()))
                .body(Resposta.ok(ModeloDto.de(modelo)));
    }
}
