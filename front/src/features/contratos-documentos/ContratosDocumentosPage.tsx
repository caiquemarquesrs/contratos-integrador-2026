import { useCallback, useEffect, useRef, useState } from "react";
import { Plus, RotateCcw, AlertCircle, RefreshCw, Bug } from "lucide-react";
import {
  Button,
  FilterBar,
  Modal,
  PageHeader,
  Pagination,
  SearchBar,
  Select,
} from "@/components/ui";
import { ContratoForm } from "@/features/contratos-documentos/components/ContratoForm";
import { ContratoDetailPanel } from "@/features/contratos-documentos/components/ContratoDetailPanel";
import {
  ContratosEmptyState,
  ContratosTable,
} from "@/features/contratos-documentos/components/ContratosTable";
import { clientes, contratos } from "@/services";
import { API_MODE } from "@/services";
import { resetDB } from "@/services/mock/db";
import { setForceErrors, getForceErrors } from "@/services/mock/helpers";
import { useDebounce } from "@/hooks/useDebounce";
import { navegar } from "@/plataforma/rotas";
import { notificarCasca } from "@/plataforma/sessao";
import {
  StatusContrato,
  STATUS_LABELS,
  getErrorMessage,
  type Cliente,
  type Contrato,
  type ContratoCreateInput,
} from "@/types/contratos";

const PAGE_SIZE = 8;

/**
 * `contratoId` vem da rota interna /contratos/{id} — a mesma rota da busca global (§8.6) e de
 * modulo:navegar (§12.2). Abrir/fechar o detalhe atualiza a rota, e a rota abre o detalhe.
 */
export function ContratosDocumentosPage({ contratoId }: { contratoId?: string }) {
  const [search, setSearch] = useState("");
  const [clienteFiltro, setClienteFiltro] = useState("");
  const [statusFiltro, setStatusFiltro] = useState("");
  const [page, setPage] = useState(1);
  const [items, setItems] = useState<Contrato[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [clientesList, setClientesList] = useState<Cliente[]>([]);
  const [clientesError, setClientesError] = useState("");

  const [showForm, setShowForm] = useState(false);
  const [detail, setDetail] = useState<Contrato | null>(null);
  const [detailError, setDetailError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [forceErrors, setForceErrorsState] = useState(getForceErrors());

  const debouncedSearch = useDebounce(search, 300);
  const requestIdRef = useRef(0);

  const hasFilters = Boolean(search || clienteFiltro || statusFiltro);

  const load = useCallback(async () => {
    const reqId = ++requestIdRef.current;
    setLoading(true);
    setError(null);
    try {
      const result = await contratos.listar({
        search: debouncedSearch,
        clienteId: clienteFiltro || undefined,
        status: (statusFiltro as StatusContrato) || undefined,
        page,
        pageSize: PAGE_SIZE,
      });
      if (reqId === requestIdRef.current) {
        setItems(result.items);
        setTotal(result.total);
      }
    } catch (e) {
      if (reqId === requestIdRef.current) {
        setError(getErrorMessage(e, "Erro ao carregar contratos."));
      }
    } finally {
      if (reqId === requestIdRef.current) setLoading(false);
    }
  }, [debouncedSearch, clienteFiltro, statusFiltro, page]);

  useEffect(() => {
    load();
  }, [load]);

  // Rota → detalhe: abre o contrato da URL (busca global, recarregar, voltar/avançar).
  useEffect(() => {
    if (!contratoId) {
      setDetail(null);
      return;
    }
    if (detail?.id === contratoId) return;
    let ativo = true;
    setDetailError("");
    contratos
      .obterPorId(contratoId)
      .then((encontrado) => {
        if (!ativo) return;
        if (encontrado) setDetail(encontrado);
        else {
          setDetailError("Contrato não encontrado.");
          navegar("/contratos", { substituir: true });
        }
      })
      .catch((cause: unknown) => {
        if (!ativo) return;
        setDetailError(getErrorMessage(cause, "Erro ao abrir o contrato."));
        navegar("/contratos", { substituir: true });
      });
    return () => {
      ativo = false;
    };
    // `detail` fica fora das dependências de propósito: só a mudança de rota dispara a busca.
  }, [contratoId]);

  // Detalhe → rota
  const abrirDetalhe = (contrato: Contrato) => {
    setDetailError("");
    setDetail(contrato);
    navegar(`/contratos/${encodeURIComponent(contrato.id)}`);
  };
  const fecharDetalhe = () => {
    setDetail(null);
    navegar("/contratos");
  };

  useEffect(() => {
    clientes.listar({}).then(setClientesList).catch((cause: unknown) => {
      setClientesError(getErrorMessage(cause, "Erro ao carregar clientes."));
    });
  }, []);

  const handleCreate = async (data: ContratoCreateInput, recaptchaToken?: string) => {
    if (!recaptchaToken) {
      throw {
        code: "RECAPTCHA_INVALIDO",
        message: "Conclua a verificação CAPTCHA antes de criar o contrato.",
      };
    }
    setSubmitting(true);
    try {
      const novo = await contratos.criar(data, recaptchaToken);
      setShowForm(false);
      setPage(1);
      abrirDetalhe(novo);
      notificarCasca("sucesso", `Contrato ${novo.numero} criado.`);
      load();
    } catch (e) {
      throw e;
    } finally {
      setSubmitting(false);
    }
  };

  const handleResetData = () => {
    resetDB();
    setPage(1);
    load();
  };

  const clearFilters = () => {
    setSearch("");
    setClienteFiltro("");
    setStatusFiltro("");
    setPage(1);
  };

  return (
    <div className="flex flex-col bg-gray-50">
      <PageHeader
        title="Contratos e Documentos"
        description={`${total} contrato(s) encontrado(s)`}
      >
        {API_MODE === "mock" && (
          <Button variant="secondary" size="sm" onClick={handleResetData}>
            <RotateCcw className="h-3 w-3" /> Resetar dados
          </Button>
        )}
        <Button size="sm" onClick={() => setShowForm(true)}>
          <Plus className="h-3 w-3" /> Novo Contrato
        </Button>
      </PageHeader>

      {API_MODE === "mock" && (
        <div className="flex flex-wrap items-center gap-3 bg-yellow-50 px-5 py-1.5 text-xs text-yellow-700">
          <span className="flex items-center gap-1.5">
            <AlertCircle className="h-3 w-3" />
            Dados simulados — modo de demonstração.
          </span>
          <button
            onClick={() => {
              const next = !forceErrors;
              setForceErrors(next);
              setForceErrorsState(next);
              load();
            }}
            className={`flex items-center gap-1 rounded-full px-2 py-0.5 font-medium transition-colors ${
              forceErrors
                ? "bg-red-100 text-red-700 hover:bg-red-200"
                : "bg-yellow-100 text-yellow-700 hover:bg-yellow-200"
            }`}
            title="Força falhas nas requisições para testar estados de erro"
          >
            <Bug className="h-3 w-3" />
            {forceErrors ? "Falhas simuladas: ON" : "Simular falhas"}
          </button>
        </div>
      )}

      <FilterBar>
        <div className="min-w-[240px] flex-1">
          <SearchBar
            value={search}
            onChange={(v) => {
              setSearch(v);
              setPage(1);
            }}
            placeholder="Buscar por número ou cliente..."
          />
        </div>
        <Select
          value={clienteFiltro}
          onChange={(e) => {
            setClienteFiltro(e.target.value);
            setPage(1);
          }}
          className="w-48"
        >
          <option value="">Todos os clientes</option>
          {clientesList.map((c) => (
            <option key={c.id} value={c.id}>{c.nome}</option>
          ))}
        </Select>
        <Select
          value={statusFiltro}
          onChange={(e) => {
            setStatusFiltro(e.target.value);
            setPage(1);
          }}
          className="w-44"
        >
          <option value="">Todos os status</option>
          {Object.values(StatusContrato).map((s) => (
            <option key={s} value={s}>{STATUS_LABELS[s]}</option>
          ))}
        </Select>
        {hasFilters && (
          <Button variant="ghost" size="sm" onClick={clearFilters}>
            Limpar filtros
          </Button>
        )}
      </FilterBar>

      {detailError && (
        <p role="alert" className="bg-red-50 px-5 py-2 text-xs text-red-700">
          {detailError}
        </p>
      )}

      {clientesError && (
        <p className="bg-amber-50 px-5 py-2 text-xs text-amber-800">
          Integração de clientes indisponível: {clientesError}
        </p>
      )}

      {/* Sem rolagem interna: a página cresce e a casca ajusta a altura do iframe (modulo:altura) */}
      <div className="bg-white">
        {error ? (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <AlertCircle className="h-12 w-12 text-red-400" />
            <p className="mt-3 text-sm text-red-600">{error}</p>
            <Button variant="outline" size="sm" className="mt-3" onClick={load}>
              <RefreshCw className="h-3 w-3" /> Tentar novamente
            </Button>
          </div>
        ) : loading ? (
          <div className="space-y-2 p-5">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="h-12 animate-pulse rounded bg-gray-100" />
            ))}
          </div>
        ) : items.length === 0 ? (
          <ContratosEmptyState hasFilters={hasFilters} />
        ) : (
          <ContratosTable items={items} onSelect={abrirDetalhe} />
        )}
      </div>

      {!loading && !error && items.length > 0 && (
        <Pagination
          page={page}
          pageSize={PAGE_SIZE}
          total={total}
          onPageChange={setPage}
        />
      )}

      {/* Modal de criação */}
      <Modal
        open={showForm}
        onClose={() => setShowForm(false)}
        title="Novo Contrato"
        subtitle="Preencha os campos obrigatórios"
        size="xl"
      >
        <ContratoForm
          onSubmit={handleCreate}
          onCancel={() => setShowForm(false)}
          submitting={submitting}
        />
      </Modal>

      {/* Modal de detalhes */}
      <Modal
        open={!!detail}
        onClose={fecharDetalhe}
        size="xl"
        noPad
      >
        {detail && (
          <ContratoDetailPanel
            contrato={detail}
            onClose={fecharDetalhe}
            onUpdated={load}
          />
        )}
      </Modal>
    </div>
  );
}
