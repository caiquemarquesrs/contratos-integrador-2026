package br.com.plataforma.contratos.item;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Toda operação verifica permissão no back-end (Contrato §5.2). Esconder botão no front é
 * usabilidade; quem chamar a API sem a permissão recebe 403.
 */
@RestController
@RequestMapping("/api/contratos/itens")
public class ItemController {

    private static final Map<String, String> CAMPOS_ORDENAVEIS = Map.of("criadoEm", "criadoEm", "nome", "nome");

    private final ItemServico servico;

    public ItemController(ItemServico servico) {
        this.servico = servico;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('contratos.item.ver')")
    public Resposta<Pagina<ItemDto>> listar(@RequestParam(defaultValue = "0") int pagina,
                                            @RequestParam(defaultValue = "20") int tamanho,
                                            @RequestParam(defaultValue = "criadoEm,desc") String ordenar) {
        PageRequest pedido = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamanho, 1, 100), ordenacao(ordenar));
        return Resposta.ok(Pagina.de(servico.listar(pedido), ItemDto::de));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('contratos.item.ver')")
    public Resposta<ItemDto> ver(@PathVariable UUID id) {
        return Resposta.ok(ItemDto.de(servico.buscar(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('contratos.item.criar')")
    public ResponseEntity<Resposta<ItemDto>> criar(@Valid @RequestBody NovoItem corpo, @AuthenticationPrincipal Jwt jwt) {
        Item item = servico.criar(corpo.nome(), corpo.descricao(), Usuarios.idDe(jwt));
        return ResponseEntity.created(URI.create("/api/contratos/itens/" + item.getId()))
                .body(Resposta.ok(ItemDto.de(item)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('contratos.item.excluir')")
    public Resposta<Void> excluir(@PathVariable UUID id) {
        servico.excluir(id);
        return Resposta.ok(null);
    }

    private static Sort ordenacao(String valor) {
        String[] partes = valor.split(",");
        String campo = CAMPOS_ORDENAVEIS.getOrDefault(partes[0].strip(), "criadoEm");
        boolean crescente = partes.length > 1 && partes[1].strip().equalsIgnoreCase("asc");
        return Sort.by(crescente ? Sort.Direction.ASC : Sort.Direction.DESC, campo);
    }
}
