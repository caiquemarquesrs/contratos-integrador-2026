package br.com.plataforma.contratos.contrato;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.plataforma.contratos.api.CampoInvalidoException;
import br.com.plataforma.contratos.api.ConflitoDeEstadoException;
import br.com.plataforma.contratos.api.NaoEncontradoException;
import br.com.plataforma.contratos.modelo.ModeloRepositorio;
import br.com.plataforma.contratos.tenant.TenantContexto;
import jakarta.persistence.EntityManager;

/**
 * Regras de contrato. Os métodos devolvem o {@link ContratoDto} já pronto: com open-in-view
 * desligado, a lista de serviços (lazy) só pode ser lida aqui dentro, na transação.
 */
@Service
public class ContratoServico {

    private final ContratoRepositorio contratos;
    private final ModeloRepositorio modelos;
    private final EntityManager entityManager;

    public ContratoServico(ContratoRepositorio contratos, ModeloRepositorio modelos, EntityManager entityManager) {
        this.contratos = contratos;
        this.modelos = modelos;
        this.entityManager = entityManager;
    }

    @Transactional
    public ContratoDto criar(NovoContrato corpo, UUID usuario) {
        TenantContexto.exigir();   // nunca gravar sem tenant definido
        conferirModelo(corpo.modeloId());

        // TODO (decisão do grupo): empresaNome deveria ser cópia da razão social no cadastro (§9.5), buscada
        // no CRM (GET /api/crm/empresas/{id}/resumo, token de serviço, timeout de 3 s, sem derrubar a
        // criação se o CRM estiver fora — §9.6). O NovoContrato não o recebe, então fica nulo por enquanto.
        Contrato contrato = Contrato.novo(proximoNumero(), corpo.empresaId(), corpo.origem(),
                corpo.origemModuloId(), corpo.dados(), usuario);
        return ContratoDto.de(contratos.save(contrato));
    }

    @Transactional(readOnly = true)
    public ContratoDto buscar(UUID id) {
        return ContratoDto.de(obter(id));
    }

    /**
     * Só rascunho e em aprovação aceitam edição. Empresa, origem e origemModuloId não mudam
     * (o EdicaoContrato nem os tem). A lista de serviços é trocada por inteira.
     */
    @Transactional
    public ContratoDto editar(UUID id, EdicaoContrato corpo, UUID usuario) {
        Contrato contrato = obter(id);
        if (!contrato.getStatus().permiteEdicao()) {
            throw new ConflitoDeEstadoException(
                    "O contrato não pode ser editado neste estado.",
                    "status",
                    "STATUS_NAO_PERMITE_EDICAO",
                    "Estado atual: " + contrato.getStatus() + ".");
        }
        conferirModelo(corpo.modeloId());
        contrato.editar(corpo.dados(), usuario);
        return ContratoDto.de(contrato);
    }

    private Contrato obter(UUID id) {
        return contratos.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Contrato não encontrado."));
    }

    /** Um modeloId informado precisa existir na empresa (o filtro de tenant vale na consulta). */
    private void conferirModelo(UUID modeloId) {
        if (modeloId != null && modelos.buscarPorId(modeloId).isEmpty()) {
            throw new CampoInvalidoException("modeloId", "MODELO_NAO_ENCONTRADO", "Modelo não encontrado.");
        }
    }

    /** CT-{ano}-{sequência de 6 dígitos}. A sequence é do banco: sem corrida entre requisições. */
    private String proximoNumero() {
        long proximo = ((Number) entityManager
                .createNativeQuery("select nextval('contratos.seq_numero_contrato')")
                .getSingleResult()).longValue();
        return String.format("CT-%d-%06d", LocalDate.now().getYear(), proximo);
    }
}
