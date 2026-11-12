/**
 * Banco de dados em memória + persistência em localStorage.
 * Namespace isolado e versionado. Tolerante a dados ausentes/corrompidos.
 * Nunca apresentado como persistência de servidor.
 */
import type {
  Cliente,
  Contrato,
  Documento,
  HistoricoEvento,
  ModeloContrato,
} from "@/types/contratos";
import { StatusContrato } from "@/types/contratos";

const STORAGE_KEY = "centinela:contratos:v1";

export interface MockDB {
  contratos: Contrato[];
  documentos: Documento[];
  clientes: Cliente[];
  historico: HistoricoEvento[];
  modelos: ModeloContrato[];
}

// --- Dados de demonstração --------------------------------------------------
const SEED_CLIENTES: Cliente[] = [
  { id: "c1", nome: "Horizonte Telecomunicações Ltda.", documento: "12345678000190", tipo: "JURIDICA", cidade: "Goiânia", uf: "GO" },
  { id: "c2", nome: "TechSoluções Serviços de TI", documento: "98765432000110", tipo: "JURIDICA", cidade: "Anápolis", uf: "GO" },
  { id: "c3", nome: "Comercial Almeida e Filhos", documento: "11222333000144", tipo: "JURIDICA", cidade: "Goiânia", uf: "GO" },
  { id: "c4", nome: "Maria Aparecida Souza", documento: "12345678901", tipo: "FISICA", cidade: "Aparecida", uf: "GO" },
  { id: "c5", nome: "Indústria de Plásticos Rio Verde", documento: "55666777000188", tipo: "JURIDICA", cidade: "Rio Verde", uf: "GO" },
  { id: "c6", nome: "João Carlos Pereira", documento: "98765432100", tipo: "FISICA", cidade: "Goiânia", uf: "GO" },
];

const SEED_MODELOS: ModeloContrato[] = [
  { id: "m1", nome: "Prestação de Serviços de TI", descricao: "Modelo padrão para serviços de tecnologia" },
  { id: "m2", nome: "Licenciamento de Software", descricao: "Modelo para licenças anuais" },
  { id: "m3", nome: "Consultoria Empresarial", descricao: "Modelo para projetos de consultoria" },
];

const hoje = new Date();
function iso(offsetDays = 0): string {
  const d = new Date(hoje);
  d.setDate(d.getDate() + offsetDays);
  return d.toISOString();
}

const SEED_CONTRATOS: Contrato[] = [
  {
    id: "ct1", numero: "CT-2026-001", clienteId: "c1", clienteNome: "Horizonte Telecomunicações Ltda.",
    servicos: [{ id: "s1", nome: "Suporte técnico mensal", valor: 3500 }],
    valor: 3500, mrr: 3500, dataInicio: iso(-120), vigenciaMeses: 12,
    renovacao: "AUTOMATICA", reajuste: "IPCA", avisoPrevioDias: 30,
    responsavel: "Ana Ferreira", status: StatusContrato.ATIVO,
    criadoEm: iso(-130), atualizadoEm: iso(-10),
  },
  {
    id: "ct2", numero: "CT-2026-002", clienteId: "c2", clienteNome: "TechSoluções Serviços de TI",
    servicos: [{ id: "s2", nome: "Desenvolvimento de sistema", valor: 28000 }],
    valor: 28000, dataInicio: iso(-30), vigenciaMeses: 6,
    renovacao: "MANUAL", reajuste: "NAO_HA", avisoPrevioDias: 15,
    responsavel: "Bruno Lima", status: StatusContrato.EM_APROVACAO,
    criadoEm: iso(-30), atualizadoEm: iso(-5),
  },
  {
    id: "ct3", numero: "CT-2026-003", clienteId: "c3", clienteNome: "Comercial Almeida e Filhos",
    servicos: [{ id: "s3", nome: "Instalação de rede", valor: 12000 }],
    valor: 12000, dataInicio: iso(-60),
    renovacao: "NAO_RENOVA", reajuste: "NAO_HA",
    responsavel: "Carla Mendes", status: StatusContrato.RASCUNHO,
    criadoEm: iso(-60), atualizadoEm: iso(-60),
  },
  {
    id: "ct4", numero: "CT-2026-004", clienteId: "c4", clienteNome: "Maria Aparecida Souza",
    servicos: [{ id: "s4", nome: "Consultoria fiscal", valor: 2500 }],
    valor: 2500, mrr: 2500, dataInicio: iso(-90), vigenciaMeses: 12,
    renovacao: "AUTOMATICA", reajuste: "IGPM", avisoPrevioDias: 30,
    responsavel: "Diego Ramos", assinaturaStatus: "PENDENTE",
    status: StatusContrato.AGUARDANDO_ASSINATURA,
    criadoEm: iso(-90), atualizadoEm: iso(-20),
  },
  {
    id: "ct5", numero: "CT-2026-005", clienteId: "c5", clienteNome: "Indústria de Plásticos Rio Verde",
    servicos: [{ id: "s5", nome: "Manutenção preventiva", valor: 8000 }],
    valor: 8000, mrr: 8000, dataInicio: iso(-200), vigenciaMeses: 24,
    renovacao: "AUTOMATICA", reajuste: "IPCA", avisoPrevioDias: 60,
    responsavel: "Elisa Nunes", assinaturaStatus: "ASSINADO", assinaturaData: iso(-195),
    status: StatusContrato.A_VENCER,
    criadoEm: iso(-200), atualizadoEm: iso(-2),
  },
  {
    id: "ct6", numero: "CT-2026-006", clienteId: "c1", clienteNome: "Horizonte Telecomunicações Ltda.",
    servicos: [{ id: "s6", nome: "Backup em nuvem", valor: 1200 }],
    valor: 1200, mrr: 1200, dataInicio: iso(-365), vigenciaMeses: 12,
    renovacao: "AUTOMATICA", reajuste: "IPCA", avisoPrevioDias: 30,
    responsavel: "Ana Ferreira", status: StatusContrato.RENOVADO,
    criadoEm: iso(-365), atualizadoEm: iso(-1),
  },
  {
    id: "ct7", numero: "CT-2026-007", clienteId: "c6", clienteNome: "João Carlos Pereira",
    servicos: [{ id: "s7", nome: "Hospedagem de site", valor: 600 }],
    valor: 600, dataInicio: iso(-45),
    renovacao: "NAO_RENOVA", reajuste: "NAO_HA",
    responsavel: "Bruno Lima", status: StatusContrato.SUSPENSO,
    criadoEm: iso(-45), atualizadoEm: iso(-15),
  },
  {
    id: "ct8", numero: "CT-2026-008", clienteId: "c2", clienteNome: "TechSoluções Serviços de TI",
    servicos: [{ id: "s8", nome: "Segurança da informação", valor: 15000 }],
    valor: 15000, dataInicio: iso(-10),
    renovacao: "MANUAL", reajuste: "NAO_HA",
    responsavel: "Carla Mendes", status: StatusContrato.CANCELADO,
    criadoEm: iso(-10), atualizadoEm: iso(-3),
  },
  {
    id: "ct9", numero: "CT-2026-009", clienteId: "c3", clienteNome: "Comercial Almeida e Filhos",
    servicos: [{ id: "s9", nome: "Treinamento de equipe", valor: 4500 }],
    valor: 4500, dataInicio: iso(-300),
    renovacao: "NAO_RENOVA", reajuste: "NAO_HA",
    responsavel: "Diego Ramos", status: StatusContrato.FINALIZADO,
    criadoEm: iso(-300), atualizadoEm: iso(-60),
  },
  {
    id: "ct10", numero: "CT-2026-010", clienteId: "c4", clienteNome: "Maria Aparecida Souza",
    servicos: [{ id: "s10", nome: "Marketing digital", valor: 3200 }],
    valor: 3200, mrr: 3200, dataInicio: iso(-15), vigenciaMeses: 12,
    renovacao: "AUTOMATICA", reajuste: "IPCA", avisoPrevioDias: 30,
    responsavel: "Elisa Nunes", status: StatusContrato.ATIVO,
    criadoEm: iso(-15), atualizadoEm: iso(-15),
  },
  {
    id: "ct11", numero: "CT-2026-011", clienteId: "c5", clienteNome: "Indústria de Plásticos Rio Verde",
    servicos: [{ id: "s11", nome: "ERP customizado", valor: 45000 }],
    valor: 45000, dataInicio: iso(-5),
    renovacao: "MANUAL", reajuste: "NAO_HA",
    responsavel: "Ana Ferreira", status: StatusContrato.RASCUNHO,
    criadoEm: iso(-5), atualizadoEm: iso(-5),
  },
  {
    id: "ct12", numero: "CT-2026-012", clienteId: "c6", clienteNome: "João Carlos Pereira",
    servicos: [{ id: "s12", nome: "Design gráfico", valor: 1800 }],
    valor: 1800, dataInicio: iso(-8),
    renovacao: "NAO_RENOVA", reajuste: "NAO_HA",
    responsavel: "Bruno Lima", status: StatusContrato.EM_APROVACAO,
    criadoEm: iso(-8), atualizadoEm: iso(-1),
  },
];

const SEED_HISTORICO: HistoricoEvento[] = [
  { id: "h1", contratoId: "ct1", dataHora: iso(-130), usuario: "Ana Ferreira", acao: "Contrato criado", simulado: true },
  { id: "h2", contratoId: "ct1", dataHora: iso(-10), usuario: "Ana Ferreira", acao: "Status alterado", campoAlterado: "status", valorAnterior: "EM_APROVACAO", valorNovo: "ATIVO", simulado: true },
  { id: "h3", contratoId: "ct2", dataHora: iso(-30), usuario: "Bruno Lima", acao: "Contrato criado", simulado: true },
  { id: "h4", contratoId: "ct5", dataHora: iso(-2), usuario: "Sistema", acao: "Status alterado", campoAlterado: "status", valorAnterior: "ATIVO", valorNovo: "A_VENCER", simulado: true },
];

export function seedDB(): MockDB {
  return {
    contratos: structuredClone(SEED_CONTRATOS),
    documentos: [],
    clientes: structuredClone(SEED_CLIENTES),
    historico: structuredClone(SEED_HISTORICO),
    modelos: structuredClone(SEED_MODELOS),
  };
}

// --- Persistência -----------------------------------------------------------
let cache: MockDB | null = null;

export function loadDB(): MockDB {
  if (cache) return cache;
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as MockDB;
      if (parsed && Array.isArray(parsed.contratos)) {
        cache = parsed;
        return cache;
      }
    }
  } catch {
    // dados corrompidos — recomeça com seed
  }
  cache = seedDB();
  saveDB();
  return cache;
}

export function saveDB(): void {
  if (!cache) return;
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(cache));
  } catch {
    // localStorage indisponível — segue em memória apenas
  }
}

export function resetDB(): MockDB {
  cache = seedDB();
  saveDB();
  return cache;
}

/** Acesso direto ao cache para os serviços mock. */
export function db(): MockDB {
  return loadDB();
}
