import { useEffect, useState } from "react";
import { Pencil, X, FileText, Download, Trash2, Plus, ArrowRightLeft, AlertCircle, Info } from "lucide-react";
import { Button, DetailField, DetailSection, Modal, Select } from "@/components/ui";
import { ContratoStatusBadge } from "@/features/contratos-documentos/components/ContratoStatusBadge";
import { ContratoForm } from "@/features/contratos-documentos/components/ContratoForm";
import { DocumentoForm } from "@/features/contratos-documentos/components/DocumentoForm";
import { ContratoHistory } from "@/features/contratos-documentos/components/ContratoHistory";
import { contratos, documentos } from "@/services";
import { API_MODE } from "@/services";
import { formatBRL, formatDate } from "@/lib/format";
import { canTransition, getAvailableStatuses } from "@/lib/status";
import {
  getErrorMessage,
  StatusContrato,
  STATUS_LABELS,
  type Contrato,
  type Documento,
} from "@/types/contratos";

interface ContratoDetailPanelProps {
  contrato: Contrato;
  onClose: () => void;
  onUpdated: () => void;
}

type Tab = "dados" | "documentos" | "historico";

export function ContratoDetailPanel({ contrato, onClose, onUpdated }: ContratoDetailPanelProps) {
  const [tab, setTab] = useState<Tab>("dados");
  const [editing, setEditing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [docs, setDocs] = useState<Documento[]>([]);
  const [docsLoading, setDocsLoading] = useState(false);
  const [docsError, setDocsError] = useState("");
  const [showDocForm, setShowDocForm] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);
  const [documentError, setDocumentError] = useState("");
  const [current, setCurrent] = useState(contrato);
  const [editError, setEditError] = useState<string | null>(null);
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [novoStatus, setNovoStatus] = useState<StatusContrato>(current.status);
  const [statusError, setStatusError] = useState("");
  const [statusSubmitting, setStatusSubmitting] = useState(false);
  const [downloadInfo, setDownloadInfo] = useState<Documento | null>(null);
  const [downloadError, setDownloadError] = useState("");

  useEffect(() => {
    if (tab === "documentos") {
      let active = true;
      setDocsLoading(true);
      setDocsError("");
      documentos.listarPorContrato(current.id).then((d) => {
        if (active) setDocs(d);
      }).catch((error: unknown) => {
        if (active) {
          setDocsError(getErrorMessage(error, "Erro ao carregar documentos."));
        }
      }).finally(() => {
        if (active) setDocsLoading(false);
      });
      return () => {
        active = false;
      };
    }
  }, [tab, current.id]);

  const handleDeleteDoc = async (id: string) => {
    setDocumentError("");
    try {
      await documentos.excluir(id);
      setConfirmDelete(null);
      setDocs((d) => d.filter((x) => x.id !== id));
    } catch (error) {
      setDocumentError(getErrorMessage(error, "Erro ao excluir documento."));
    }
  };

  const handleAlterarStatus = async () => {
    if (novoStatus === current.status) return;
    if (!canTransition(current.status, novoStatus, API_MODE === "real")) {
      setStatusError("Transição de status não permitida.");
      return;
    }
    setStatusSubmitting(true);
    setStatusError("");
    try {
      const updated = await contratos.alterarStatus(current.id, { status: novoStatus });
      setCurrent(updated);
      setShowStatusModal(false);
      onUpdated();
    } catch (e) {
      setStatusError(getErrorMessage(e, "Erro ao alterar status."));
    } finally {
      setStatusSubmitting(false);
    }
  };

  const handleDownload = async (doc: Documento) => {
    setDownloadError("");
    if (API_MODE === "mock") {
      setDownloadInfo(doc);
      return;
    }
    try {
      await documentos.baixar(doc.id);
    } catch (error) {
      setDownloadInfo(doc);
      setDownloadError(getErrorMessage(error, "Erro ao baixar documento."));
    }
  };

  const tabs: { key: Tab; label: string }[] = [
    { key: "dados", label: "Dados" },
    { key: "documentos", label: `Documentos (${docs.length})` },
    { key: "historico", label: "Histórico" },
  ];

  if (editing) {
    return (
      <div className="p-5">
        {editError && (
          <div className="mb-4 flex items-center gap-2 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            <AlertCircle className="h-4 w-4" />
            {editError}
          </div>
        )}
        <ContratoForm
          initial={current}
          onSubmit={async (data) => {
            setSubmitting(true);
            setEditError(null);
            try {
              const updated = await contratos.atualizar(current.id, data);
              setCurrent(updated);
              setEditing(false);
              onUpdated();
            } catch (e) {
              setEditError(getErrorMessage(e, "Erro ao atualizar contrato."));
              // relança para o formulário capturar fieldErrors, se houver
              throw e;
            } finally {
              setSubmitting(false);
            }
          }}
          onCancel={() => { setEditing(false); setEditError(null); }}
          submitting={submitting}
        />
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col overflow-hidden bg-white">
      {/* Header colorido */}
      <div className="bg-brand-950 px-5 pb-3 pt-5 text-white shrink-0">
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-lg font-bold">{current.numero}</h2>
            <p className="mt-0.5 text-sm text-brand-300">
              {current.clienteNome || (API_MODE === "real" ? "Empresa não retornada pela API" : "—")}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <ContratoStatusBadge status={current.status} />
            <Button variant="secondary" size="sm" onClick={() => { setNovoStatus(current.status); setStatusError(""); setShowStatusModal(true); }}>
              <ArrowRightLeft className="h-3 w-3" /> Alterar Status
            </Button>
            <Button variant="secondary" size="sm" onClick={() => setEditing(true)}>
              <Pencil className="h-3 w-3" /> Editar
            </Button>
            <button
              onClick={onClose}
              className="rounded-lg p-1.5 text-brand-300 hover:bg-brand-800 hover:text-white"
              aria-label="Fechar"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex shrink-0 overflow-x-auto border-b border-gray-100 bg-white">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`whitespace-nowrap border-b-2 px-4 py-2.5 text-xs font-semibold transition-colors ${
              tab === t.key
                ? "border-brand-700 text-brand-800"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Conteúdo */}
      <div className="flex-1 overflow-y-auto p-5">
        {tab === "dados" && (
          <>
            <DetailSection number={1} title="Identificação">
              <DetailField label="Número" value={current.numero} />
              <DetailField label="Cliente" value={current.clienteNome} />
              <DetailField
                label="Responsável"
                value={API_MODE === "real" ? "Nome não retornado pela API" : current.responsavel || "—"}
              />
              <DetailField label="Status">
                <ContratoStatusBadge status={current.status} />
              </DetailField>
            </DetailSection>

            <DetailSection number={2} title="Serviços e Valores">
              <DetailField
                label="Serviços"
                value={current.servicos.map((s) => s.nome).join(", ") || "—"}
                colSpan={2}
              />
              <DetailField
                label={API_MODE === "real" ? "Valor mensal" : "Valor total"}
                value={
                  API_MODE === "real"
                    ? current.valorMensal != null ? formatBRL(current.valorMensal) : "—"
                    : current.valor != null ? formatBRL(current.valor) : "—"
                }
              />
              <DetailField label="MRR" value={current.mrr != null ? formatBRL(current.mrr) : "—"} />
            </DetailSection>

            <DetailSection number={3} title="Vigência">
              <DetailField label="Início" value={formatDate(current.dataInicio)} />
              <DetailField
                label="Vigência"
                value={current.vigenciaMeses ? `${current.vigenciaMeses} meses` : "—"}
              />
              <DetailField label="Renovação" value={renovacaoLabel(current.renovacao)} />
              <DetailField label="Reajuste" value={reajusteLabel(current.reajuste)} />
              <DetailField label="Índice" value={current.indiceReajuste || "—"} />
              {current.reajusteAnualPercentual != null && (
                <DetailField
                  label="Reajuste anual"
                  value={`${current.reajusteAnualPercentual}%`}
                />
              )}
              <DetailField
                label="Aviso prévio"
                value={current.avisoPrevioDias ? `${current.avisoPrevioDias} dias` : "—"}
              />
            </DetailSection>

            <DetailSection number={4} title="Assinatura">
              <DetailField
                label="Status"
                value={assinaturaLabel(current.assinaturaStatus)}
              />
            </DetailSection>
          </>
        )}

        {tab === "documentos" && (
          <div>
            <div className="mb-4 flex justify-end">
              <Button size="sm" onClick={() => setShowDocForm(true)}>
                <Plus className="h-3 w-3" /> Adicionar documento
              </Button>
            </div>
            {docsError ? (
              <p className="text-sm text-red-600">{docsError}</p>
            ) : docsLoading ? (
              <p className="text-sm text-gray-400">Carregando documentos...</p>
            ) : docs.length === 0 ? (
              <div className="py-12 text-center">
                <FileText className="mx-auto h-10 w-10 text-gray-300" />
                <p className="mt-2 text-sm text-gray-500">Nenhum documento associado.</p>
              </div>
            ) : (
              <ul className="divide-y divide-gray-100 rounded-lg border border-gray-100">
                {docs.map((d) => (
                  <li key={d.id} className="flex items-center justify-between px-4 py-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-gray-900">{d.nome}</p>
                      <p className="text-xs text-gray-400">
                        {d.tipo} · enviado em {formatDate(d.enviadoEm)}
                      </p>
                    </div>
                    <div className="flex gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        aria-label="Baixar documento"
                        onClick={() => handleDownload(d)}
                      >
                        <Download className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        aria-label="Excluir documento"
                        onClick={() => {
                          setDocumentError("");
                          setConfirmDelete(d.id);
                        }}
                      >
                        <Trash2 className="h-4 w-4 text-red-500" />
                      </Button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        {tab === "historico" && <ContratoHistory contratoId={current.id} />}
      </div>

      {/* Modal de adicionar documento */}
      <Modal
        open={showDocForm}
        onClose={() => setShowDocForm(false)}
        title="Adicionar documento"
        subtitle={current.numero}
        size="lg"
      >
        <DocumentoForm
          contratoId={current.id}
          onCancel={() => setShowDocForm(false)}
          onSuccess={(doc) => {
            setDocs((d) => [...d, doc]);
            setShowDocForm(false);
          }}
        />
      </Modal>

      {/* Confirmação de exclusão */}
      <Modal
        open={!!confirmDelete}
        onClose={() => setConfirmDelete(null)}
        title="Excluir documento?"
        size="sm"
        footer={
          <>
            <Button variant="outline" onClick={() => setConfirmDelete(null)}>
              Cancelar
            </Button>
            <Button variant="danger" onClick={() => confirmDelete && handleDeleteDoc(confirmDelete)}>
              Excluir
            </Button>
          </>
        }
      >
        <p className="text-sm text-gray-600">
          Esta ação não pode ser desfeita. O documento será removido do contrato.
        </p>
        {documentError && <p className="mt-2 text-sm text-red-600">{documentError}</p>}
      </Modal>

      <Modal
        open={!!downloadInfo}
        onClose={() => { setDownloadInfo(null); setDownloadError(""); }}
        title={downloadInfo?.nome ?? "Download"}
        size="sm"
        footer={
          <Button variant="outline" onClick={() => setDownloadInfo(null)}>
            Fechar
          </Button>
        }
      >
        {downloadInfo && (
          <div className="space-y-3">
            {downloadError && (
              <p className="text-sm text-red-600">{downloadError}</p>
            )}
            {API_MODE === "mock" ? (
              <div className="flex gap-2 rounded-lg bg-yellow-50 p-3 text-sm text-yellow-800">
                <Info className="h-4 w-4 shrink-0" />
                <p>
                  No modo de demonstração, apenas os <strong>metadados</strong> do arquivo são
                  armazenados. O conteúdo real não é persistido.
                </p>
              </div>
            ) : null}
            <div className="space-y-1 text-sm text-gray-600">
              <p><span className="font-medium text-gray-700">Nome:</span> {downloadInfo.nome}</p>
              <p><span className="font-medium text-gray-700">Tipo:</span> {downloadInfo.tipo}</p>
              {downloadInfo.arquivoNome && (
                <p><span className="font-medium text-gray-700">Arquivo:</span> {downloadInfo.arquivoNome}</p>
              )}
              {downloadInfo.arquivoTamanho != null && (
                <p><span className="font-medium text-gray-700">Tamanho:</span> {(downloadInfo.arquivoTamanho / 1024).toFixed(1)} KB</p>
              )}
              {downloadInfo.arquivoTipo && (
                <p><span className="font-medium text-gray-700">MIME:</span> {downloadInfo.arquivoTipo}</p>
              )}
            </div>
          </div>
        )}
      </Modal>

      {/* Modal de alteração de status */}
      <Modal
        open={showStatusModal}
        onClose={() => !statusSubmitting && setShowStatusModal(false)}
        title="Alterar status do contrato"
        subtitle={current.numero}
        size="md"
        footer={
          <>
            <Button variant="outline" onClick={() => setShowStatusModal(false)} disabled={statusSubmitting}>
              Cancelar
            </Button>
            <Button onClick={handleAlterarStatus} loading={statusSubmitting} disabled={statusSubmitting || novoStatus === current.status}>
              Confirmar alteração
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="flex items-center gap-3 rounded-lg bg-gray-50 p-3">
            <span className="text-sm text-gray-500">Status atual:</span>
            <ContratoStatusBadge status={current.status} />
          </div>
          {getAvailableStatuses(current.status, API_MODE === "real").length > 0 ? <div>
            <label className="mb-1.5 block text-sm font-medium text-gray-700">
              Novo status
            </label>
            <Select
              value={novoStatus}
              onChange={(e) => setNovoStatus(e.target.value as StatusContrato)}
            >
              {getAvailableStatuses(current.status, API_MODE === "real").map((s) => (
                <option key={s} value={s}>{STATUS_LABELS[s]}</option>
              ))}
            </Select>
          </div> : (
            <p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
              A API não oferece uma operação de status compatível com o estado atual do contrato.
            </p>
          )}
          {statusError && (
            <p className="flex items-center gap-1.5 text-xs text-red-600">
              <AlertCircle className="h-3.5 w-3.5" />
              {statusError}
            </p>
          )}
          <p className="text-xs text-gray-400">
            A alteração será registrada no histórico do contrato.
          </p>
        </div>
      </Modal>
    </div>
  );
}

function renovacaoLabel(r: string): string {
  return { AUTOMATICA: "Automática", MANUAL: "Manual", NAO_RENOVA: "Não renova" }[r] ?? r;
}
function reajusteLabel(r: string): string {
  return { IPCA: "IPCA", IGPM: "IGP-M", SELIC: "SELIC", NAO_HA: "Não há", OUTRO: "Outro" }[r] ?? r;
}
function assinaturaLabel(s?: string): string {
  return { PENDENTE: "Pendente", ASSINADO: "Assinado", NAO_APLICAVEL: "Não aplicável" }[s ?? ""] ?? "—";
}
