import { useEffect, useState } from "react";
import { Clock } from "lucide-react";
import { historico } from "@/services";
import { formatDateTime } from "@/lib/format";
import { getErrorMessage, type HistoricoEvento } from "@/types/contratos";

interface ContratoHistoryProps {
  contratoId: string;
}

export function ContratoHistory({ contratoId }: ContratoHistoryProps) {
  const [events, setEvents] = useState<HistoricoEvento[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    historico.listarPorContrato(contratoId).then((r) => {
      if (active) setEvents(r);
    }).catch((cause: unknown) => {
      if (active) {
        setError(getErrorMessage(cause, "Erro ao carregar o histórico."));
      }
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => {
      active = false;
    };
  }, [contratoId]);

  if (loading) return <p className="text-sm text-gray-400">Carregando histórico...</p>;
  if (error) return <p className="text-sm text-red-600">{error}</p>;

  if (events.length === 0) {
    return (
      <div className="py-12 text-center">
        <Clock className="mx-auto h-10 w-10 text-gray-300" />
        <p className="mt-2 text-sm text-gray-500">Nenhuma alteração registrada.</p>
      </div>
    );
  }

  return (
    <ol className="relative space-y-4 border-l border-gray-200 pl-4">
      {events.map((e) => (
        <li key={e.id} className="relative">
          <span className="absolute -left-[21px] top-1 flex h-3 w-3 rounded-full bg-brand-700 ring-4 ring-white" />
          <div className="rounded-lg border border-gray-100 bg-gray-50 px-3 py-2">
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium text-gray-900">{e.acao}</p>
              {e.simulado && (
                <span className="rounded-full bg-yellow-50 px-2 py-0.5 text-[10px] font-medium text-yellow-700">
                  Simulado
                </span>
              )}
            </div>
            {e.campoAlterado && (
              <p className="mt-1 text-xs text-gray-600">
                <span className="font-medium">{e.campoAlterado}:</span>{" "}
                {e.valorAnterior} → {e.valorNovo}
              </p>
            )}
            <p className="mt-1 text-xs text-gray-400">
              {e.usuarioId
                ? `ID do usuário: ${e.usuarioId}`
                : e.usuario} · {formatDateTime(e.dataHora)}
            </p>
          </div>
        </li>
      ))}
    </ol>
  );
}
