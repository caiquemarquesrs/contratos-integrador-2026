import type { ContratosService } from "@/services/contracts";
import { http, type ApiPage } from "@/services/real/http";
import { sessaoAtual } from "@/plataforma/sessao";
import type {
  Contrato,
  ContratoCreateInput,
  PaginatedResult,
  StatusContrato,
} from "@/types/contratos";

interface ApiContrato {
  id: string;
  numero: string;
  empresaId: string;
  empresaNome: string | null;
  modeloId: string | null;
  servicosContratados: {
    itemCatalogoId: string | null;
    descricao: string;
    quantidade: number;
    valorUnitario: number;
    valorTotal: number;
  }[];
  valorMensal: number;
  mrr: number;
  dataInicio: string;
  vigenciaMeses: number | null;
  renovacao: "automatica" | "manual" | "sem_renovacao";
  reajusteAnualPercentual: number | null;
  indiceReajuste: string | null;
  avisoPrevioDias: number;
  responsavelId: string;
  status: StatusContrato;
  assinatura: { assinadoEm: string; assinadoPor: string } | null;
  criadoEm: string;
  atualizadoEm: string;
}

function fromApi(contract: ApiContrato): Contrato {
  const semReajuste = contract.indiceReajuste === "nenhum";
  return {
    id: contract.id,
    numero: contract.numero,
    clienteId: contract.empresaId,
    clienteNome: contract.empresaNome ?? undefined,
    servicos: contract.servicosContratados.map((service, index) => ({
      id: service.itemCatalogoId ?? `${contract.id}-servico-${index}`,
      nome: service.descricao,
      valor: service.valorTotal,
    })),
    valorMensal: contract.valorMensal,
    mrr: contract.mrr,
    dataInicio: contract.dataInicio,
    vigenciaMeses: contract.vigenciaMeses ?? undefined,
    renovacao:
      contract.renovacao === "sem_renovacao"
        ? "NAO_RENOVA"
        : contract.renovacao.toUpperCase() as "AUTOMATICA" | "MANUAL",
    reajuste: semReajuste
      ? "NAO_HA"
      : contract.indiceReajuste === "IPCA" || contract.indiceReajuste === "IGPM"
        ? contract.indiceReajuste
        : "OUTRO",
    reajusteAnualPercentual: contract.reajusteAnualPercentual ?? undefined,
    indiceReajuste: contract.indiceReajuste ?? undefined,
    avisoPrevioDias: contract.avisoPrevioDias,
    responsavel: "",
    responsavelId: contract.responsavelId,
    assinaturaData: contract.assinatura?.assinadoEm,
    assinaturaResponsavel: contract.assinatura?.assinadoPor,
    assinaturaStatus: contract.assinatura ? "ASSINADO" : "PENDENTE",
    modeloId: contract.modeloId ?? undefined,
    status: contract.status,
    criadoEm: contract.criadoEm,
    atualizadoEm: contract.atualizadoEm,
  };
}

function incompatibilidadeDeGravacao(): never {
  throw {
    code: "INTEGRATION_CONTRACT_MISMATCH",
    message:
      "A edição real está bloqueada: a API exige responsávelId UUID e quantidade dos serviços, " +
      "enquanto a tela mantém responsável textual e serviços sem quantidade. Alinhe os campos " +
      "com o grupo responsável antes de gravar.",
  };
}

/** Renovação: a tela usa maiúsculas; a API usa minúsculas, com "sem_renovacao". */
const RENOVACAO_PARA_API = {
  AUTOMATICA: "automatica",
  MANUAL: "manual",
  NAO_RENOVA: "sem_renovacao",
} as const;

/**
 * Corpo do POST /contratos/contratos (schema NovoContrato).
 *
 * Passagens diretas do formulário, sem inventar valor de negócio:
 * - empresaId ← cliente selecionado na tela (a API chama o cliente de empresa);
 * - valorMensal ← "Valor total" da tela; mrr ← campo MRR (sem preenchimento, repete o mensal);
 * - avisoPrevioDias ← campo da tela (sem preenchimento, fica 0);
 * - cada serviço vira quantidade 1, com valor unitário e total iguais ao valor informado.
 * Os valores não são arredondados: a API rejeita mais de 2 casas com PRECISAO_EXCEDIDA.
 */
function corpoDoNovoContrato(input: ContratoCreateInput) {
  return {
    empresaId: input.clienteId,
    modeloId: input.modeloId ?? null,
    servicosContratados: input.servicos
      .filter((servico) => servico.nome.trim())
      .map((servico) => ({
        descricao: servico.nome.trim(),
        quantidade: 1,
        valorUnitario: servico.valor ?? 0,
        valorTotal: servico.valor ?? 0,
      })),
    valorMensal: input.valor,
    mrr: input.mrr ?? input.valor,
    dataInicio: input.dataInicio,
    vigenciaMeses: input.vigenciaMeses ?? null,
    renovacao: RENOVACAO_PARA_API[input.renovacao],
    avisoPrevioDias: input.avisoPrevioDias ?? 0,
    responsavelId: idDoUsuarioAtual(),
    origem: "contratos",
    origemModuloId: `form-novo-contrato-${crypto.randomUUID()}`,
  };
}

function idDoUsuarioAtual(): string {
  const id = sessaoAtual()?.usuario.id;
  if (!id || !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(id)) {
    throw {
      code: "AUTHENTICATED_USER_ID_REQUIRED",
      message:
        "A operação exige uma sessão autenticada com identificador de usuário UUID.",
    };
  }
  return id;
}

export const contratosReal: ContratosService = {
  async listar(filters, signal) {
    const params = new URLSearchParams({
      pagina: String(filters.page - 1),
      tamanho: String(filters.pageSize),
    });
    const search = filters.search?.trim();
    if (search && search.length < 2) {
      throw {
        code: "CAMPO_INVALIDO",
        message: "A API exige pelo menos 2 caracteres na pesquisa.",
        fieldErrors: { q: "Informe ao menos 2 caracteres." },
      };
    }
    if (search) params.set("q", search);
    if (filters.clienteId) params.set("empresaId", filters.clienteId);
    if (filters.status) params.set("status", filters.status);
    const page = await http.get<ApiPage<ApiContrato>>(
      `/contratos/contratos?${params}`,
      signal,
    );
    return {
      items: page.itens.map(fromApi),
      page: page.pagina + 1,
      pageSize: page.tamanho,
      total: page.total,
    } satisfies PaginatedResult<Contrato>;
  },
  async obterPorId(id) {
    const contract = await http.get<ApiContrato>(
      `/contratos/contratos/${encodeURIComponent(id)}`,
    );
    return fromApi(contract);
  },
  async criar(input, recaptchaToken) {
    const contrato = await http.post<ApiContrato>(
      "/contratos/contratos",
      corpoDoNovoContrato(input),
      { "X-Recaptcha-Token": recaptchaToken },
    );
    return fromApi(contrato);
  },
  atualizar(_id, _input) {
    return incompatibilidadeDeGravacao();
  },
  async alterarStatus(id, input) {
    const path = `/contratos/contratos/${encodeURIComponent(id)}`;
    const contract =
      input.status === "AGUARDANDO_ASSINATURA"
        ? await http.post<ApiContrato>(`${path}/aprovar`)
        : input.status === "ATIVO"
          ? await http.post<ApiContrato>(`${path}/assinar`, {
              assinadoPor: idDoUsuarioAtual(),
            })
          : (() => {
              throw {
                code: "UNSUPPORTED_STATUS_OPERATION",
                message:
                  "A API não oferece uma operação para alterar diretamente para este status.",
              };
            })();
    return fromApi(contract);
  },
};
