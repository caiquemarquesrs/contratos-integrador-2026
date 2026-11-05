package br.com.plataforma.contratos.contrato;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.plataforma.contratos.api.CampoInvalidoException;
import br.com.plataforma.contratos.api.ConflitoDeEstadoException;
import br.com.plataforma.contratos.api.NaoEncontradoException;
import br.com.plataforma.contratos.api.Pagina;
import br.com.plataforma.contratos.eventos.FatoOcorrido;
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
    private final ApplicationEventPublisher eventos;
    private final HistoricoServico historico;

    public ContratoServico(ContratoRepositorio contratos, ModeloRepositorio modelos, EntityManager entityManager,
                           ApplicationEventPublisher eventos, HistoricoServico historico) {
        this.contratos = contratos;
        this.modelos = modelos;
        this.entityManager = entityManager;
        this.eventos = eventos;
        this.historico = historico;
    }

    @Transactional
    public ContratoDto criar(NovoContrato corpo, UUID usuario, String empresaNome) {
        TenantContexto.exigir();   // nunca gravar sem tenant definido
        conferirModelo(corpo.modeloId());

        // empresaNome: cópia da razão social no momento do cadastro (§9.5), buscada no CRM pelo controller,
        // fora desta transação. Vem nulo se o CRM não respondeu (§9.6).
        Contrato contrato = Contrato.novo(proximoNumero(), corpo.empresaId(), empresaNome, corpo.origem(),
                corpo.origemModuloId(), corpo.dados(), usuario);
        Contrato salvo = contratos.save(contrato);
        historico.registrar(salvo.getId(), "status", null, StatusContrato.RASCUNHO.name(), usuario);
        return ContratoDto.de(salvo);
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
        Contrato.Dados antes = contrato.dados();
        contrato.editar(corpo.dados(), usuario);
        // uma linha por campo que mudou de verdade; editar sem mudar nada não registra nada
        historico.registrar(id, ComparadorDeContrato.comparar(antes, contrato.dados()), usuario);
        return ContratoDto.de(contrato);
    }

    /** Histórico de mudanças, do mais recente para o mais antigo. 404 se o contrato não existir ou for de outra empresa. */
    @Transactional(readOnly = true)
    public Pagina<HistoricoDto> historico(UUID id, int pagina, int tamanho) {
        obter(id);
        return historico.listar(id, pagina, tamanho);
    }

    /**
     * EM_APROVACAO -> AGUARDANDO_ASSINATURA. Em qualquer outro estado a resposta é 409.
     * A transição RASCUNHO -> EM_APROVACAO não existe no contratos.yaml (decisão do grupo: não alterar a infra).
     */
    @Transactional
    public ContratoDto aprovar(UUID id, UUID usuario) {
        Contrato contrato = obter(id);
        exigirStatus(contrato, StatusContrato.EM_APROVACAO, "aprovado");
        StatusContrato anterior = contrato.getStatus();
        contrato.aprovar(usuario);
        historico.registrar(id, "status", anterior.name(), contrato.getStatus().name(), usuario);
        return ContratoDto.de(contrato);
    }

    /**
     * AGUARDANDO_ASSINATURA -> ATIVO, registrando quem assinou e quando. Em qualquer outro estado a resposta é 409.
     * O documentoAssinadoId do corpo é aceito, mas ainda não é gravado: os documentos ainda não existem no
     * banco (estão com o Caique). Quando existirem, é aqui que o vínculo será feito.
     */
    @Transactional
    public ContratoDto assinar(UUID id, RegistroAssinatura corpo, UUID usuario) {
        Contrato contrato = obter(id);
        exigirStatus(contrato, StatusContrato.AGUARDANDO_ASSINATURA, "assinado");
        StatusContrato anterior = contrato.getStatus();
        contrato.assinar(corpo.assinadoPor(), usuario);
        historico.registrar(id, "status", anterior.name(), contrato.getStatus().name(), usuario);

        // O Financeiro começa a cobrança recorrente a partir deste evento (contratos.asyncapi.yaml). O fato é
        // registrado dentro da transação; o PublicadorDeEventos só o envia ao RabbitMQ depois do commit, então
        // um 409, um 400 ou um rollback nunca publicam nada. "dados" tem só estes três campos.
        eventos.publishEvent(new FatoOcorrido(
                "contratos.contrato.assinado", contrato.getTenantId(), usuario,
                Map.of("contratoId", contrato.getId(),
                       "empresaId", contrato.getEmpresaId(),
                       "valorMensal", contrato.getValorMensal().setScale(2, RoundingMode.UNNECESSARY))));
        return ContratoDto.de(contrato);
    }

    /** 409 no formato do exemplo ConflitoDeEstado do contratos.yaml. */
    private void exigirStatus(Contrato contrato, StatusContrato esperado, String acao) {
        if (contrato.getStatus() != esperado) {
            throw new ConflitoDeEstadoException(
                    "O contrato precisa estar em " + esperado + " para ser " + acao + ".",
                    "status",
                    "TRANSICAO_INVALIDA",
                    "Estado atual: " + contrato.getStatus() + ".");
        }
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
