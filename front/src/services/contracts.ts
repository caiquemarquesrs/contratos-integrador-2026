/**
 * Contratos de serviço — interfaces compartilhadas entre mock e real.
 * Nenhum componente importa implementação concreta; só estas interfaces.
 */
import type {
  AlterarStatusInput,
  Cliente,
  ClienteFilters,
  Contrato,
  ContratoCreateInput,
  ContratoFilters,
  ContratoUpdateInput,
  Documento,
  DocumentoCreateInput,
  HistoricoEvento,
  ModeloContrato,
  PaginatedResult,
} from "@/types/contratos";

export interface ContratosService {
  listar(
    filters: ContratoFilters & { page: number; pageSize: number },
    signal?: AbortSignal
  ): Promise<PaginatedResult<Contrato>>;
  obterPorId(id: string): Promise<Contrato | null>;
  criar(input: ContratoCreateInput, recaptchaToken: string): Promise<Contrato>;
  atualizar(id: string, input: ContratoUpdateInput): Promise<Contrato>;
  alterarStatus(id: string, input: AlterarStatusInput): Promise<Contrato>;
}

export interface DocumentosService {
  listarPorContrato(contratoId: string): Promise<Documento[]>;
  adicionar(input: DocumentoCreateInput): Promise<Documento>;
  excluir(id: string): Promise<void>;
  baixar(id: string): Promise<void>;
}

export interface ClientesService {
  listar(filters: ClienteFilters): Promise<Cliente[]>;
  obterPorId(id: string): Promise<Cliente | null>;
}

export interface HistoricoService {
  listarPorContrato(contratoId: string): Promise<HistoricoEvento[]>;
}

export interface ModelosService {
  listar(): Promise<ModeloContrato[]>;
}

export interface Services {
  contratos: ContratosService;
  documentos: DocumentosService;
  clientes: ClientesService;
  historico: HistoricoService;
  modelos: ModelosService;
}
