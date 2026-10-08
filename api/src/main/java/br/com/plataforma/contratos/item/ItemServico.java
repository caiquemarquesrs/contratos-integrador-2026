package br.com.plataforma.contratos.item;

import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.plataforma.contratos.api.NaoEncontradoException;
import br.com.plataforma.contratos.eventos.FatoOcorrido;
import br.com.plataforma.contratos.tenant.TenantContexto;

@Service
public class ItemServico {

    private final ItemRepositorio itens;
    private final ApplicationEventPublisher publicador;

    public ItemServico(ItemRepositorio itens, ApplicationEventPublisher publicador) {
        this.itens = itens;
        this.publicador = publicador;
    }

    @Transactional(readOnly = true)
    public Page<Item> listar(Pageable pagina) {
        return itens.findAll(pagina);
    }

    @Transactional(readOnly = true)
    public Item buscar(UUID id) {
        return itens.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Item não encontrado."));
    }

    @Transactional
    public Item criar(String nome, String descricao, UUID usuario) {
        UUID tenant = TenantContexto.exigir();   // nunca gravar sem tenant definido
        Item item = itens.save(Item.novo(nome.strip(), descricao, usuario));

        // O evento só sai depois do commit — ver PublicadorDeEventos.
        publicador.publishEvent(new FatoOcorrido(
                "contratos.item.criado", tenant, usuario,
                Map.of("itemId", item.getId(), "nome", item.getNome())));
        return item;
    }

    @Transactional
    public void excluir(UUID id) {
        itens.delete(buscar(id));
    }
}
