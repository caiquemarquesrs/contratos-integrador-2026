package br.com.plataforma.contratos.modelo;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.plataforma.contratos.api.NaoEncontradoException;
import br.com.plataforma.contratos.tenant.TenantContexto;

@Service
public class ModeloServico {

    private final ModeloRepositorio modelos;

    public ModeloServico(ModeloRepositorio modelos) {
        this.modelos = modelos;
    }

    @Transactional(readOnly = true)
    public Page<Modelo> listar(Pageable pagina) {
        return modelos.findAll(pagina);
    }

    @Transactional(readOnly = true)
    public Modelo buscar(UUID id) {
        return modelos.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Modelo não encontrado."));
    }

    @Transactional
    public Modelo criar(String nome, String descricao, String conteudo, UUID usuario) {
        TenantContexto.exigir();   // nunca gravar sem tenant definido
        return modelos.save(Modelo.novo(nome.strip(), descricao, conteudo, usuario));
    }
}
