package br.com.plataforma.contratos.contrato;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.plataforma.contratos.api.CampoInvalidoException;
import br.com.plataforma.contratos.api.ConflitoDeEstadoException;
import br.com.plataforma.contratos.api.NaoEncontradoException;
import br.com.plataforma.contratos.api.Pagina;
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
    public ContratoDto criar(NovoContrato corpo, UUID usuario, String empresaNome) {
        TenantContexto.exigir();   // nunca gravar sem tenant definido
        conferirModelo(corpo.modeloId());

        // empresaNome: cópia da razão social no momento do cadastro (§9.5), buscada no CRM pelo controller,
        // fora desta transação. Vem nulo se o CRM não respondeu (§9.6).
        Contrato contrato = Contrato.novo(proximoNumero(), corpo.empresaId(), empresaNome, corpo.origem(),
                corpo.origemModuloId(), corpo.dados(), usuario);
        return ContratoDto.de(contratos.save(contrato));
    }

    /** Ordenações aceitas em "ordenar". O id desempata, para a paginação não repetir nem perder itens. */
    private static final Map<String, Sort> ORDENACOES = Map.of(
            "criadoEm,desc", Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.asc("id")),
            "criadoEm,asc", Sort.by(Sort.Order.asc("criadoEm"), Sort.Order.asc("id")),
            "vigenciaFim,asc", Sort.by(Sort.Order.asc("vigenciaFim"), Sort.Order.asc("id")),
            "valorMensal,desc", Sort.by(Sort.Order.desc("valorMensal"), Sort.Order.asc("id")));

    /**
     * Lista os contratos da empresa do token. {@code q} (mínimo 2 caracteres) procura no número ou no
     * nome da empresa, sem diferenciar maiúscula de minúscula. Parâmetro inválido responde 400.
     */
    @Transactional(readOnly = true)
    public Pagina<ContratoDto> listar(String q, UUID empresaId, StatusContrato status, String ordenar,
                                      int pagina, int tamanho) {
        if (q != null && q.strip().length() < 2) {
            throw new CampoInvalidoException("q", "CAMPO_INVALIDO", "A busca precisa de ao menos 2 caracteres.");
        }
        Sort ordem = ORDENACOES.get(ordenar);
        if (ordem == null) {
            throw new CampoInvalidoException("ordenar", "CAMPO_INVALIDO",
                    "Use criadoEm,desc, criadoEm,asc, vigenciaFim,asc ou valorMensal,desc.");
        }

        Specification<Contrato> filtros = (raiz, consulta, cb) -> cb.conjunction();
        if (empresaId != null) {
            filtros = filtros.and((raiz, consulta, cb) -> cb.equal(raiz.get("empresaId"), empresaId));
        }
        if (status != null) {
            filtros = filtros.and((raiz, consulta, cb) -> cb.equal(raiz.get("status"), status));
        }
        if (q != null) {
            String padrao = "%" + q.strip().toLowerCase()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            filtros = filtros.and((raiz, consulta, cb) -> cb.or(
                    cb.like(cb.lower(raiz.get("numero")), padrao, '\\'),
                    cb.like(cb.lower(raiz.get("empresaNome")), padrao, '\\')));
        }

        return Pagina.de(contratos.findAll(filtros, PageRequest.of(pagina, tamanho, ordem)), ContratoDto::de);
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
