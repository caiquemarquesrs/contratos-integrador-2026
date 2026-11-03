package br.com.plataforma.contratos.integracao;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.plataforma.contratos.api.CampoInvalidoException;
import br.com.plataforma.contratos.api.NaoEncontradoException;
import br.com.plataforma.contratos.contrato.Contrato;
import br.com.plataforma.contratos.contrato.ContratoRepositorio;
import br.com.plataforma.contratos.contrato.StatusContrato;
import br.com.plataforma.contratos.integracao.StatusContratual.Situacao;

/**
 * Endpoints de leitura que outros módulos consomem (busca global, resumo e status contratual).
 * Tudo aqui é só consulta: nada é gravado. Todas as consultas passam pelo filtro de tenant do
 * Hibernate, então contrato de outra empresa nunca aparece (nem como 403: simplesmente não existe).
 */
@Service
@Transactional(readOnly = true)
public class IntegracaoServico {

    static final int MAXIMO_DE_IDS = 100;
    static final int MAXIMO_DA_BUSCA = 5;
    static final int MINIMO_DA_BUSCA = 2;

    /** Texto de reserva quando o nome da empresa não está gravado no contrato (§9.6). */
    static final String EMPRESA_INDISPONIVEL = "Empresa indisponível";

    /** Caractere de escape do LIKE na busca (veja ContratoRepositorio.buscarPorTexto). */
    private static final char ESCAPE_DO_LIKE = '!';

    /** Estados em que o contrato conta para o status contratual da empresa. */
    private static final Set<StatusContrato> VIGENTES = EnumSet.of(
            StatusContrato.ATIVO, StatusContrato.A_VENCER, StatusContrato.RENOVADO, StatusContrato.SUSPENSO);

    private final ContratoRepositorio contratos;

    public IntegracaoServico(ContratoRepositorio contratos) {
        this.contratos = contratos;
    }

    // ---------------------------------------------------------------- resumo

    public ContratoResumo resumo(UUID id) {
        return contratos.buscarPorId(id)
                .map(IntegracaoServico::resumoDe)
                .orElseThrow(() -> new NaoEncontradoException("Contrato não encontrado."));
    }

    /** Só os que existem na empresa, na ordem pedida e sem repetir; id desconhecido não é erro. */
    public List<ContratoResumo> resumos(List<UUID> ids) {
        exigirNoMaximoCemIds(ids);
        Set<UUID> pedidos = new LinkedHashSet<>(ids);
        if (pedidos.isEmpty()) {
            return List.of();
        }
        Map<UUID, Contrato> encontrados = contratos.buscarPorIds(pedidos).stream()
                .collect(Collectors.toMap(Contrato::getId, Function.identity()));
        return pedidos.stream()
                .map(encontrados::get)
                .filter(Objects::nonNull)
                .map(IntegracaoServico::resumoDe)
                .toList();
    }

    private static ContratoResumo resumoDe(Contrato c) {
        return new ContratoResumo(c.getId(), c.getNumero(), nomeDaEmpresa(c), c.getStatus(),
                c.getValorMensal(), c.getVigenciaFim());
    }

    // ---------------------------------------------------------------- busca

    /** Até 5 contratos cujo número ou nome da empresa contém o texto, do mais recente para o mais antigo. */
    public List<ContratoBusca> buscar(String q) {
        String termo = q == null ? "" : q.strip();
        if (termo.length() < MINIMO_DA_BUSCA) {
            throw new CampoInvalidoException("q", "CAMPO_INVALIDO",
                    "Informe ao menos " + MINIMO_DA_BUSCA + " caracteres para buscar.");
        }
        return contratos.buscarPorTexto(padraoDeBusca(termo), PageRequest.of(0, MAXIMO_DA_BUSCA)).stream()
                .map(IntegracaoServico::buscaDe)
                .toList();
    }

    /**
     * Texto digitado vira "%texto%" em minúsculas. Os curingas do LIKE que o usuário digitou
     * ("%" e "_") e o próprio caractere de escape são escapados: "50%" procura mesmo o "%".
     */
    static String padraoDeBusca(String termo) {
        String escapado = termo.toLowerCase(Locale.ROOT)
                .replace(String.valueOf(ESCAPE_DO_LIKE), "" + ESCAPE_DO_LIKE + ESCAPE_DO_LIKE)
                .replace("%", ESCAPE_DO_LIKE + "%")
                .replace("_", ESCAPE_DO_LIKE + "_");
        return "%" + escapado + "%";
    }

    private static ContratoBusca buscaDe(Contrato c) {
        String titulo = temNome(c) ? c.getNumero() + " — " + c.getEmpresaNome() : c.getNumero();
        String subtitulo = rotulo(c.getStatus()) + " · " + dinheiro(c.getValorMensal()) + "/mês";
        return new ContratoBusca(c.getId(), titulo, subtitulo, "/contratos/" + c.getId());
    }

    private static String rotulo(StatusContrato status) {
        return switch (status) {
            case RASCUNHO -> "Rascunho";
            case EM_APROVACAO -> "Em aprovação";
            case AGUARDANDO_ASSINATURA -> "Aguardando assinatura";
            case ATIVO -> "Ativo";
            case A_VENCER -> "A vencer";
            case RENOVADO -> "Renovado";
            case SUSPENSO -> "Suspenso";
            case CANCELADO -> "Cancelado";
            case FINALIZADO -> "Finalizado";
        };
    }

    /**
     * "R$ 1.890,00" com espaço comum. Os separadores são fixos (não dependem dos dados de idioma
     * do JVM, que mudam de uma instalação para outra) e o espaço depois do "R$" nunca é o
     *   que o NumberFormat de pt-BR devolve.
     */
    static String dinheiro(BigDecimal valor) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.ROOT);
        simbolos.setDecimalSeparator(',');
        simbolos.setGroupingSeparator('.');
        return "R$ " + new DecimalFormat("#,##0.00", simbolos).format(valor);
    }

    // ---------------------------------------------------------------- status contratual

    /** Sempre devolve um status, mesmo sem contrato (sem_contrato). */
    public StatusContratual statusContratual(UUID empresaId) {
        return statusContratuais(List.of(empresaId), LocalDate.now()).get(0);
    }

    /** Um item para cada id pedido, na mesma ordem, inclusive as empresas sem contrato. */
    public List<StatusContratual> statusContratuais(List<UUID> empresaIds) {
        exigirNoMaximoCemIds(empresaIds);
        return statusContratuais(empresaIds, LocalDate.now());
    }

    /** "hoje" entra como parâmetro para o cálculo poder ser testado com datas fixas. */
    List<StatusContratual> statusContratuais(List<UUID> empresaIds, LocalDate hoje) {
        if (empresaIds.isEmpty()) {
            return List.of();
        }
        // uma única consulta para todas as empresas do lote; o agrupamento é em memória
        Map<UUID, List<Contrato>> porEmpresa = contratos
                .vigentesDasEmpresas(new LinkedHashSet<>(empresaIds), VIGENTES).stream()
                .collect(Collectors.groupingBy(Contrato::getEmpresaId));
        return empresaIds.stream()
                .map(id -> statusDaEmpresa(id, porEmpresa.getOrDefault(id, List.of()), hoje))
                .toList();
    }

    private record Avaliado(Contrato contrato, Situacao situacao) {
    }

    /** Do mais grave para o menos grave; em empate, vence o de menor vigenciaFim (sem vigenciaFim por último). */
    private static final Comparator<Avaliado> PIOR_PRIMEIRO = Comparator
            .comparing(Avaliado::situacao, Comparator.<Situacao>reverseOrder())
            .thenComparing(a -> a.contrato().getVigenciaFim(), Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
            .thenComparing(a -> a.contrato().getNumero());   // só para o resultado nunca depender da ordem do banco

    /*
     * REGRAS DO STATUS CONTRATUAL (decisão do grupo; o contratos.yaml só dá uma linha por situação).
     *
     * 1. Contratos vigentes da empresa: ATIVO, A_VENCER, RENOVADO e SUSPENSO. Rascunho, em
     *    aprovação, aguardando assinatura, cancelado e finalizado não contam. Nenhum vigente =>
     *    sem_contrato (contratoId, numero e diasParaVencer nulos).
     * 2. Situação de cada contrato vigente:
     *      critico: SUSPENSO; ou vigenciaFim já passou e a renovação não é "automatica"
     *               (vencido sem renovação);
     *      atencao: A_VENCER; ou hoje >= vigenciaFim - avisoPrevioDias (janela de aviso prévio);
     *               ou vigenciaFim já passou e a renovação é "automatica" (aguardando renovação);
     *      ok:      nenhum dos casos acima (inclui contrato sem vigenciaFim).
     * 3. A empresa fica com a pior situação entre os contratos (critico > atencao > ok). Em empate
     *    vale o contrato de menor vigenciaFim (sem vigenciaFim por último); ele preenche
     *    contratoId e numero.
     * 4. diasParaVencer = dias entre hoje e a vigenciaFim desse contrato: nulo sem vigenciaFim,
     *    0 se já venceu.
     * 5. "Já passou" é vigenciaFim anterior a hoje: no próprio dia do fim o contrato ainda vale
     *    (diasParaVencer = 0) e está na janela de aviso, ou seja, atencao.
     */
    static StatusContratual statusDaEmpresa(UUID empresaId, List<Contrato> vigentes, LocalDate hoje) {
        return vigentes.stream()
                .map(c -> new Avaliado(c, situacaoDoContrato(
                        c.getStatus(), c.getVigenciaFim(), c.getRenovacao(), c.getAvisoPrevioDias(), hoje)))
                .min(PIOR_PRIMEIRO)
                .map(a -> new StatusContratual(empresaId, a.situacao(), a.contrato().getId(),
                        a.contrato().getNumero(), diasParaVencer(a.contrato().getVigenciaFim(), hoje)))
                .orElseGet(() -> StatusContratual.semContrato(empresaId));
    }

    /** Regra 2 acima, sem depender da entidade (testada em RegrasDeStatusTest). */
    static Situacao situacaoDoContrato(StatusContrato status, LocalDate vigenciaFim, String renovacao,
                                       int avisoPrevioDias, LocalDate hoje) {
        boolean vencido = vigenciaFim != null && vigenciaFim.isBefore(hoje);
        if (status == StatusContrato.SUSPENSO || (vencido && !"automatica".equals(renovacao))) {
            return Situacao.CRITICO;
        }
        boolean naJanelaDeAviso = vigenciaFim != null && !hoje.isBefore(vigenciaFim.minusDays(avisoPrevioDias));
        if (status == StatusContrato.A_VENCER || naJanelaDeAviso || vencido) {
            return Situacao.ATENCAO;
        }
        return Situacao.OK;
    }

    /** Regra 4 acima. */
    static Integer diasParaVencer(LocalDate vigenciaFim, LocalDate hoje) {
        if (vigenciaFim == null) {
            return null;
        }
        return (int) Math.max(0, ChronoUnit.DAYS.between(hoje, vigenciaFim));
    }

    // ---------------------------------------------------------------- apoio

    /** Método único do nome da empresa: o mesmo texto de reserva no resumo, no lote e na busca (§9.6). */
    static String nomeDaEmpresa(Contrato c) {
        return temNome(c) ? c.getEmpresaNome() : EMPRESA_INDISPONIVEL;
    }

    private static boolean temNome(Contrato c) {
        return c.getEmpresaNome() != null && !c.getEmpresaNome().isBlank();
    }

    private static void exigirNoMaximoCemIds(List<UUID> ids) {
        if (ids.size() > MAXIMO_DE_IDS) {
            throw new CampoInvalidoException("ids", "CAMPO_INVALIDO",
                    "Informe no máximo " + MAXIMO_DE_IDS + " ids por chamada.");
        }
    }
}
