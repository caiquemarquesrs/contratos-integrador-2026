import { FileText, Pencil, Eye } from "lucide-react";
import { Button } from "@/components/ui";
import { ContratoStatusBadge } from "@/features/contratos-documentos/components/ContratoStatusBadge";
import { formatBRL, formatDate } from "@/lib/format";
import { API_MODE } from "@/services";
import type { Contrato } from "@/types/contratos";

interface ContratosTableProps {
  items: Contrato[];
  onSelect: (contrato: Contrato) => void;
}

export function ContratosTable({ items, onSelect }: ContratosTableProps) {
  return (
    <div className="overflow-x-auto p-3">
      <div className="min-w-[980px] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full border-separate border-spacing-0 text-left text-sm">
          <thead>
            <tr className="bg-slate-50 text-[11px] font-semibold uppercase tracking-[0.08em] text-slate-500">
              <th className="px-4 py-3 text-left">Número</th>
              <th className="px-4 py-3 text-left">Cliente</th>
              <th className="px-4 py-3 text-left">Serviço(s)</th>
              <th className="px-4 py-3 text-right">{API_MODE === "real" ? "Valor mensal" : "Valor"}</th>
              <th className="px-4 py-3 text-left">Vigência</th>
              <th className="px-4 py-3 text-left">Status</th>
              <th className="px-4 py-3 text-left">Responsável</th>
              <th className="px-4 py-3 text-right">Ações</th>
            </tr>
          </thead>
          <tbody>
            {items.map((c) => (
              <tr
                key={c.id}
                className="cursor-pointer border-b border-slate-100 bg-white transition-colors hover:bg-slate-50/80"
                onClick={() => onSelect(c)}
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    onSelect(c);
                  }
                }}
              >
                <td className="border-b border-slate-100 px-4 py-3 align-middle font-semibold text-brand-950">
                  {c.numero}
                </td>
                <td className="border-b border-slate-100 px-4 py-3 align-middle text-slate-700">
                  {c.clienteNome ?? "—"}
                </td>
                <td className="max-w-[220px] border-b border-slate-100 px-4 py-3 align-middle text-slate-700">
                  <span className="block truncate">{c.servicos.map((s) => s.nome).join(", ") || "—"}</span>
                </td>
                <td className="border-b border-slate-100 px-4 py-3 align-middle text-right font-semibold text-slate-900">
                  {API_MODE === "real"
                    ? c.valorMensal != null ? formatBRL(c.valorMensal) : "—"
                    : c.valor != null ? formatBRL(c.valor) : "—"}
                </td>
                <td className="border-b border-slate-100 px-4 py-3 align-middle text-slate-600">
                  {formatDate(c.dataInicio)}
                  {c.vigenciaMeses ? ` · ${c.vigenciaMeses} meses` : ""}
                </td>
                <td className="border-b border-slate-100 px-4 py-3 align-middle">
                  <ContratoStatusBadge status={c.status} />
                </td>
                <td className="border-b border-slate-100 px-4 py-3 align-middle text-slate-600">
                  {API_MODE === "real" ? "—" : c.responsavel || "—"}
                </td>
                <td className="border-b border-slate-100 px-4 py-3 align-middle">
                  <div className="flex justify-end gap-1" onClick={(e) => e.stopPropagation()}>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 rounded-lg p-0 text-slate-500 hover:bg-slate-100 hover:text-brand-900"
                      onClick={() => onSelect(c)}
                      aria-label="Ver detalhes"
                    >
                      <Eye className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-8 w-8 rounded-lg p-0 text-slate-500 hover:bg-slate-100 hover:text-brand-900"
                      onClick={() => onSelect(c)}
                      aria-label="Editar contrato"
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export function ContratosEmptyState({ hasFilters }: { hasFilters: boolean }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <FileText className="h-12 w-12 text-gray-300" />
      <h3 className="mt-3 text-sm font-semibold text-gray-700">
        {hasFilters ? "Nenhum contrato encontrado" : "Nenhum contrato cadastrado"}
      </h3>
      <p className="mt-1 max-w-sm text-xs text-gray-400">
        {hasFilters
          ? "Tente ajustar os filtros ou a busca para encontrar o que procura."
          : "Clique em 'Novo Contrato' para registrar o primeiro contrato."}
      </p>
    </div>
  );
}
