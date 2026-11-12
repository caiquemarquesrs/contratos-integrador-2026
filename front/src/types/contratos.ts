/**
 * Tipos do módulo Contratos e Documentos.
 * Compartilhados entre a implementação mock e a real.
 */

// --- Status do contrato -----------------------------------------------------
// Valores internos estáveis (sem espaços, sem acentos).
// O mapeamento para rótulo e badge é centralizado em ContratoStatusBadge.
export enum StatusContrato {
  RASCUNHO = "RASCUNHO",
  EM_APROVACAO = "EM_APROVACAO",
  AGUARDANDO_ASSINATURA = "AGUARDANDO_ASSINATURA",
  ATIVO = "ATIVO",
  A_VENCER = "A_VENCER",
  RENOVADO = "RENOVADO",
  SUSPENSO = "SUSPENSO",
  CANCELADO = "CANCELADO",
  FINALIZADO = "FINALIZADO",
}

export const STATUS_LABELS: Record<StatusContrato, string> = {
  [StatusContrato.RASCUNHO]: "Rascunho",
  [StatusContrato.EM_APROVACAO]: "Em Aprovação",
  [StatusContrato.AGUARDANDO_ASSINATURA]: "Aguardando Assinatura",
  [StatusContrato.ATIVO]: "Ativo",
  [StatusContrato.A_VENCER]: "A Vencer",
  [StatusContrato.RENOVADO]: "Renovado",
  [StatusContrato.SUSPENSO]: "Suspenso",
  [StatusContrato.CANCELADO]: "Cancelado",
  [StatusContrato.FINALIZADO]: "Finalizado",
};

// --- Cliente (mínimo para seleção) -----------------------------------------
export interface Cliente {
  id: string;
  nome: string;
  documento: string; // CPF ou CNPJ (apenas dígitos)
  tipo: "FISICA" | "JURIDICA";
  cidade?: string;
  uf?: string;
}

// --- Serviço comercial ------------------------------------------------------
export interface ServicoContratado {
  id: string;
  nome: string;
  valor?: number;
}

// --- Modelo de contrato -----------------------------------------------------
export interface ModeloContrato {
  id: string;
  nome: string;
  descricao?: string;
}

// --- Documento --------------------------------------------------------------
export interface Documento {
  id: string;
  contratoId: string;
  nome: string;
  tipo: string; // categoria, ex.: "CONTRATO", "ANEXO", "CERTIDAO"
  descricao?: string;
  // Apenas metadados do arquivo — nunca o conteúdo no mock.
  arquivoNome?: string;
  arquivoTamanho?: number; // bytes
  arquivoTipo?: string; // MIME
  enviadoEm: string; // ISO
  atualizadoEm?: string; // ISO
  responsavel?: string;
}

// --- Histórico --------------------------------------------------------------
export interface HistoricoEvento {
  id: string;
  contratoId: string;
  dataHora: string; // ISO
  usuario: string;
  usuarioId?: string;
  acao: string;
  campoAlterado?: string;
  valorAnterior?: string;
  valorNovo?: string;
  simulado: boolean; // sempre true no mock
}

// --- Contrato ---------------------------------------------------------------
export interface Contrato {
  id: string;
  numero: string;
  clienteId: string;
  clienteNome?: string; // preenchido na listagem/detalhes
  servicos: ServicoContratado[];
  valor?: number; // valor total usado pelo mock
  valorMensal?: number; // campo retornado pela API real
  mrr?: number;
  dataInicio: string; // ISO date
  vigenciaMeses?: number;
  dataFim?: string; // ISO date
  renovacao: "AUTOMATICA" | "MANUAL" | "NAO_RENOVA";
  reajuste: "IPCA" | "IGPM" | "SELIC" | "NAO_HA" | "OUTRO";
  reajusteAnualPercentual?: number;
  indiceReajuste?: string;
  avisoPrevioDias?: number;
  responsavel: string; // nome usado pelo mock; a API real não retorna o nome
  responsavelId?: string; // identificador retornado pela API real
  // Informações de assinatura (campos informativos)
  assinaturaData?: string;
  assinaturaResponsavel?: string;
  assinaturaStatus?: "PENDENTE" | "ASSINADO" | "NAO_APLICAVEL";
  modeloId?: string;
  status: StatusContrato;
  criadoEm: string;
  atualizadoEm: string;
}

// --- DTOs de entrada --------------------------------------------------------
export type ContratoCreateInput = Omit<
  Contrato,
  | "id"
  | "criadoEm"
  | "atualizadoEm"
  | "clienteNome"
  | "valorMensal"
  | "responsavelId"
  | "reajusteAnualPercentual"
> & { valor: number };

export type ContratoUpdateInput = Partial<ContratoCreateInput>;

export interface AlterarStatusInput {
  status: StatusContrato;
}

export interface DocumentoCreateInput {
  contratoId: string;
  nome: string;
  tipo: string;
  descricao?: string;
  arquivo?: File;
  arquivoNome?: string;
  arquivoTamanho?: number;
  arquivoTipo?: string;
}

// --- Resultado paginado -----------------------------------------------------
export interface PaginatedResult<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
}

// --- Filtros de listagem ----------------------------------------------------
export interface ContratoFilters {
  search?: string; // número ou nome do cliente
  clienteId?: string;
  status?: StatusContrato;
}

export interface ClienteFilters {
  search?: string;
}

// --- Erro normalizado -------------------------------------------------------
export interface ApiError {
  code: string;
  message: string;
  fieldErrors?: Record<string, string>;
}

export function isApiError(error: unknown): error is ApiError {
  return (
    typeof error === "object" &&
    error !== null &&
    "code" in error &&
    typeof error.code === "string" &&
    "message" in error &&
    typeof error.message === "string"
  );
}

export function getErrorMessage(error: unknown, fallback: string): string {
  if (isApiError(error)) return error.message;
  if (error instanceof Error) return error.message;
  return fallback;
}
