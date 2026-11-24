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

/** Índice de reajuste: a API usa IPCA/IGPM/INPC/nenhum; a tela usa NAO_HA para "nenhum". */
const REAJUSTE_DA_API: Record<string, Contrato["reajuste"]> = {
  IPCA: "IPCA",
  IGPM: "IGPM",
  INPC: "INPC",
  nenhum: "NAO_HA",
};

const REAJUSTE_PARA_API: Partial<Record<Contrato["reajuste"], string>> = {
  IPCA: "IPCA",
  IGPM: "IGPM",
  INPC: "INPC",
  NAO_HA: "nenhum",
};

function reajusteParaApi(reajuste: Contrato["reajuste"]): string {
  const valor = REAJUSTE_PARA_API[reajuste];
  if (!valor) {
    throw {
      code: "CAMPO_INVALIDO",
      message: "A API aceita apenas IPCA, IGP-M, INPC ou sem reajuste. Escolha uma dessas opções.",
      fieldErrors: { reajuste: "Escolha IPCA, IGP-M, INPC ou Não há." },
    };
  }
  return valor;
}

function fromApi(contract: ApiContrato): Contrato {
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
    reajuste: REAJUSTE_DA_API[contract.indiceReajuste ?? "nenhum"] ?? "OUTRO",
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
 * - cada serviço vira quantidade 1, com valor unitário e total iguais ao valor informado;
 * - reajuste ← índice escolhido (IPCA, IGP-M, INPC ou nenhum) e percentual anual opcional;
 * - número e status não são enviados: a API gera o número e o contrato nasce em EM_APROVACAO.
 * Os valores não são arredondados: a API rejeita mais de 2 casas com PRECISAO_EXCEDIDA.
 */
function corpoDoNovoContrato(input: ContratoCreateInput) {
  return {
    empresaId: input.clienteId,
    ...dadosEditaveis(input, idDoUsuarioAtual()),
    origem: "contratos",
    origemModuloId: `form-novo-contrato-${crypto.randomUUID()}`,
  };
}

/**
 * Campos comuns ao NovoContrato e à EdicaoContrato. Empresa, origem e origemModuloId não mudam
 * depois de criado, então ficam fora (a API nem os aceita no PUT).
 */
function dadosEditaveis(input: ContratoCreateInput, responsavelId: string) {
  return {
    modeloId: input.modeloId || null,
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
    reajusteAnualPercentual: input.reajusteAnualPercentual ?? null,
    indiceReajuste: reajusteParaApi(input.reajuste),
    avisoPrevioDias: input.avisoPrevioDias ?? 0,
    responsavelId,
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
  /** PUT /contratos/contratos/{id}: só RASCUNHO e EM_APROVACAO aceitam edição (senão 409). */
  async atualizar(id, input) {
    const contrato = await http.put<ApiContrato>(
      `/contratos/contratos/${encodeURIComponent(id)}`,
      dadosEditaveis(input as ContratoCreateInput, input.responsavelId ?? idDoUsuarioAtual()),
    );
    return fromApi(contrato);
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
