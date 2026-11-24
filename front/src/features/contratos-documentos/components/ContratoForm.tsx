import { useCallback, useEffect, useMemo, useState } from "react";
import { Button, FormField, FormSection, Input, Select, Modal } from "@/components/ui";
import { ClienteSelector } from "@/features/contratos-documentos/components/ClienteSelector";
import { RecaptchaCheckbox } from "@/features/contratos-documentos/components/RecaptchaCheckbox";
import { API_MODE, clientes, modelos } from "@/services";
import { getErrorMessage, isApiError } from "@/types/contratos";
import {
  StatusContrato,
  STATUS_LABELS,
  type Cliente,
  type Contrato,
  type ContratoCreateInput,
  type ModeloContrato,
} from "@/types/contratos";

interface ContratoFormProps {
  initial?: Contrato | null;
  onSubmit: (data: ContratoCreateInput, recaptchaToken?: string) => Promise<void>;
  onCancel: () => void;
  submitting?: boolean;
}

type FormState = Omit<ContratoCreateInput, "servicos"> & {
  servicos: { nome: string; valor?: string }[];
};

const RENOVACAO_OPTIONS = [
  { value: "AUTOMATICA", label: "Automática" },
  { value: "MANUAL", label: "Manual" },
  { value: "NAO_RENOVA", label: "Não renova" },
];

const REAJUSTE_OPTIONS = [
  { value: "IPCA", label: "IPCA" },
  { value: "IGPM", label: "IGP-M" },
  { value: "INPC", label: "INPC" },
  { value: "SELIC", label: "SELIC" },
  { value: "NAO_HA", label: "Não há" },
  { value: "OUTRO", label: "Outro" },
];

// A API aceita apenas IPCA, IGPM, INPC e nenhum (Contrato de Integração do contratos.yaml).
const REAJUSTE_OPTIONS_REAL = REAJUSTE_OPTIONS.filter((o) =>
  ["IPCA", "IGPM", "INPC", "NAO_HA"].includes(o.value),
);

function emptyForm(): FormState {
  return {
    numero: "",
    clienteId: "",
    valor: 0,
    mrr: undefined,
    dataInicio: new Date().toISOString().slice(0, 10),
    vigenciaMeses: undefined,
    renovacao: "AUTOMATICA",
    reajuste: "IPCA",
    reajusteAnualPercentual: undefined,
    indiceReajuste: "",
    avisoPrevioDias: 30,
    responsavel: "",
    assinaturaStatus: "PENDENTE",
    modeloId: "",
    status: StatusContrato.EM_APROVACAO,
    servicos: [{ nome: "", valor: "" }],
  };
}

function toInput(state: FormState): ContratoCreateInput {
  return {
    numero: state.numero.trim(),
    clienteId: state.clienteId,
    servicos: state.servicos
      .filter((s) => s.nome.trim())
      .map((s, i) => ({
        id: `svc_${Date.now()}_${i}`,
        nome: s.nome.trim(),
        valor: s.valor ? Number(s.valor) : undefined,
      })),
    valor: Number(state.valor) || 0,
    mrr: state.mrr !== undefined && state.mrr !== null ? Number(state.mrr) : undefined,
    dataInicio: state.dataInicio,
    vigenciaMeses: state.vigenciaMeses ? Number(state.vigenciaMeses) : undefined,
    renovacao: state.renovacao,
    reajuste: state.reajuste,
    indiceReajuste: state.indiceReajuste?.trim() || undefined,
    reajusteAnualPercentual:
      state.reajusteAnualPercentual !== undefined && state.reajusteAnualPercentual !== null
        ? Number(state.reajusteAnualPercentual)
        : undefined,
    avisoPrevioDias: state.avisoPrevioDias ? Number(state.avisoPrevioDias) : undefined,
    responsavel: state.responsavel.trim(),
    assinaturaStatus: state.assinaturaStatus,
    modeloId: state.modeloId || undefined,
    status: state.status,
  };
}

export function ContratoForm({ initial, onSubmit, onCancel, submitting }: ContratoFormProps) {
  const [form, setForm] = useState<FormState>(() => {
    if (!initial) return emptyForm();
    return {
      numero: initial.numero,
      clienteId: initial.clienteId,
      // a API real devolve valorMensal; o mock usa valor
      valor: initial.valor ?? initial.valorMensal ?? 0,
      mrr: initial.mrr,
      dataInicio: initial.dataInicio.slice(0, 10),
      vigenciaMeses: initial.vigenciaMeses,
      renovacao: initial.renovacao,
      reajuste: initial.reajuste,
      indiceReajuste: initial.indiceReajuste ?? "",
      reajusteAnualPercentual: initial.reajusteAnualPercentual,
      avisoPrevioDias: initial.avisoPrevioDias,
      responsavel: initial.responsavel,
      assinaturaStatus: initial.assinaturaStatus ?? "PENDENTE",
      modeloId: initial.modeloId ?? "",
      status: initial.status,
      servicos:
        initial.servicos.length > 0
          ? initial.servicos.map((s) => ({ nome: s.nome, valor: s.valor?.toString() ?? "" }))
          : [{ nome: "", valor: "" }],
    };
  });

  const [cliente, setCliente] = useState<Cliente | null>(null);
  const [clienteError, setClienteError] = useState("");
  const [modelosList, setModelosList] = useState<ModeloContrato[]>([]);
  const [modelosError, setModelosError] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [recaptchaToken, setRecaptchaToken] = useState<string | null>(null);
  const [recaptchaResetKey, setRecaptchaResetKey] = useState(0);
  const [submitError, setSubmitError] = useState("");

  const isEdit = !!initial;
  /** Na API real, empresa, status e assinatura não mudam pelo PUT: aparecem só para leitura. */
  const somenteLeituraReal = API_MODE === "real" && isEdit;

  // Estado inicial serializado para comparação de dirty
  const initialSnapshot = useMemo(() => JSON.stringify(form), []);
  const isDirty = JSON.stringify(form) !== initialSnapshot;

  useEffect(() => {
    if (initial) {
      clientesObter(initial.clienteId).then(setCliente).catch((cause: unknown) => {
        setClienteError(getErrorMessage(cause, "Erro ao carregar cliente."));
      });
    }
  }, [initial]);

  useEffect(() => {
    modelos.listar().then(setModelosList).catch((cause: unknown) => {
      setModelosError(getErrorMessage(cause, "Erro ao carregar modelos."));
    });
  }, []);

  const upd = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => ({ ...e, [key]: "" }));
    setServerErrors((e) => ({ ...e, [key]: "" }));
  };

  const addServico = () =>
    setForm((f) => ({ ...f, servicos: [...f.servicos, { nome: "", valor: "" }] }));
  const removeServico = (i: number) =>
    setForm((f) => ({
      ...f,
      servicos: f.servicos.filter((_, idx) => idx !== i),
    }));
  const updServico = (i: number, field: "nome" | "valor", value: string) =>
    setForm((f) => ({
      ...f,
      servicos: f.servicos.map((s, idx) => (idx === i ? { ...s, [field]: value } : s)),
    }));

  const handleRecaptchaToken = useCallback((token: string | null) => {
    setRecaptchaToken(token);
    if (token) setSubmitError("");
  }, []);

  const validate = (): boolean => {
    const e: Record<string, string> = {};
    // No modo real, a API gera o número e registra o usuário logado como responsável.
    if (API_MODE === "mock" && !form.numero.trim()) e.numero = "Número do contrato é obrigatório.";
    if (!form.clienteId) e.clienteId = "Selecione um cliente.";
    if (API_MODE === "mock" && !form.responsavel.trim()) e.responsavel = "Responsável é obrigatório.";
    if (API_MODE === "real" && !form.servicos.some((s) => s.nome.trim()))
      e.servicos = "Informe ao menos um serviço contratado.";
    if (!form.dataInicio) e.dataInicio = "Data de início é obrigatória.";
    if (form.valor < 0) e.valor = "O valor não pode ser negativo.";
    if (form.mrr !== undefined && form.mrr !== null && Number(form.mrr) < 0)
      e.mrr = "O MRR não pode ser negativo.";
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleCancel = () => {
    if (isDirty && !submitting) {
      setConfirmCancel(true);
    } else {
      onCancel();
    }
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    if (!isEdit && !recaptchaToken) return;
    setSubmitError("");
    try {
      await onSubmit(toInput(form), recaptchaToken ?? undefined);
    } catch (e) {
      if (isApiError(e) && e.fieldErrors) {
        setServerErrors(e.fieldErrors);
      }
      setSubmitError(getErrorMessage(e, isEdit ? "Erro ao salvar contrato." : "Erro ao criar contrato."));
      if (!isEdit) {
        setRecaptchaToken(null);
        setRecaptchaResetKey((current) => current + 1);
      }
    }
  };

  const fieldErr = (key: keyof FormState): string | undefined =>
    errors[key as string] || serverErrors[key as string];

  return (
    <div className="space-y-2">
      {somenteLeituraReal && (
        <div className="rounded-lg bg-slate-50 p-3 text-sm text-slate-600">
          Só contratos em Rascunho ou Em Aprovação podem ser editados. Cliente e status não mudam por aqui:
          o status muda pelo botão “Alterar Status”.
        </div>
      )}
      <FormSection number={1} title="Identificação">
        <FormField
          label="Número do contrato"
          required={API_MODE === "mock"}
          error={fieldErr("numero")}
          hint={API_MODE === "real" ? "Gerado automaticamente pela API ao salvar." : undefined}
        >
          <Input
            value={API_MODE === "real" && !isEdit ? "" : form.numero}
            onChange={(e) => upd("numero", e.target.value)}
            placeholder={API_MODE === "real" ? "Automático" : "Ex.: CT-2026-001"}
            disabled={API_MODE === "real"}
          />
        </FormField>
        <FormField
          label="Status"
          hint={API_MODE === "real" && !isEdit ? "Novos contratos começam Em Aprovação." : undefined}
        >
          <Select
            value={form.status}
            onChange={(e) => upd("status", e.target.value as StatusContrato)}
            disabled={API_MODE === "real"}
          >
            {Object.values(StatusContrato).map((s) => (
              <option key={s} value={s}>{STATUS_LABELS[s]}</option>
            ))}
          </Select>
        </FormField>
        <FormField label="Cliente" required error={fieldErr("clienteId") || clienteError} colSpan={2}>
          {somenteLeituraReal ? (
            <Input value={cliente?.nome ?? initial?.clienteNome ?? ""} disabled />
          ) : (
            <ClienteSelector
              value={cliente}
              onChange={(c) => {
                setCliente(c);
                upd("clienteId", c?.id ?? "");
              }}
              required
            />
          )}
        </FormField>
        <FormField
          label="Responsável"
          required={API_MODE === "mock"}
          error={fieldErr("responsavel")}
          hint={API_MODE === "real" ? (isEdit ? "Mantido o responsável atual." : "Registrado como o usuário logado.") : undefined}
        >
          <Input
            value={API_MODE === "real" ? (isEdit ? "Responsável atual" : "Usuário logado") : form.responsavel}
            onChange={(e) => upd("responsavel", e.target.value)}
            placeholder="Nome do responsável"
            disabled={API_MODE === "real"}
          />
        </FormField>
        <FormField label="Modelo de contrato" error={modelosError}>
          <Select
            value={form.modeloId}
            onChange={(e) => upd("modeloId", e.target.value)}
          >
            <option value="">Nenhum</option>
            {modelosList.map((m) => (
              <option key={m.id} value={m.id}>{m.nome}</option>
            ))}
          </Select>
        </FormField>
      </FormSection>

      <FormSection number={2} title="Serviços e Valores">
        <div className="md:col-span-2 space-y-2">
          {form.servicos.map((s, i) => (
            <div key={i} className="flex gap-2">
              {/* wrapper com min-w-0: o Input tem w-full e, sem isso, o flex encolhe o campo a ~30px */}
              <div className="min-w-0 flex-1">
                <Input
                  value={s.nome}
                  onChange={(e) => updServico(i, "nome", e.target.value)}
                  placeholder="Nome do serviço"
                />
              </div>
              {/* largura fixa no wrapper: o className do Input não vence o w-full da base */}
              <div className="w-32 shrink-0">
                <Input
                  value={s.valor}
                  onChange={(e) => updServico(i, "valor", e.target.value)}
                  placeholder="Valor (R$)"
                  type="number"
                />
              </div>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => removeServico(i)}
                disabled={form.servicos.length === 1}
              >
                Remover
              </Button>
            </div>
          ))}
          <Button type="button" variant="secondary" size="sm" onClick={addServico}>
            + Adicionar serviço
          </Button>
          {fieldErr("servicos") && (
            <p className="text-sm text-red-600">{fieldErr("servicos")}</p>
          )}
        </div>
        <FormField
          label={API_MODE === "real" ? "Valor mensal (R$)" : "Valor total (R$)"}
          required
          error={fieldErr("valor") || serverErrors.valorMensal}
        >
          <Input
            type="number"
            value={form.valor}
            onChange={(e) => upd("valor", Number(e.target.value) as never)}
          />
        </FormField>
        <FormField label="MRR (R$)" error={fieldErr("mrr")} hint="Receita recorrente mensal, quando aplicável">
          <Input
            type="number"
            value={form.mrr ?? ""}
            onChange={(e) => upd("mrr", e.target.value ? Number(e.target.value) : undefined)}
          />
        </FormField>
      </FormSection>

      <FormSection number={3} title="Vigência e Reajuste">
        <FormField label="Data de início" required error={fieldErr("dataInicio")}>
          <Input
            type="date"
            value={form.dataInicio}
            onChange={(e) => upd("dataInicio", e.target.value)}
          />
        </FormField>
        <FormField label="Vigência (meses)">
          <Input
            type="number"
            value={form.vigenciaMeses ?? ""}
            onChange={(e) => upd("vigenciaMeses", e.target.value ? Number(e.target.value) : undefined)}
          />
        </FormField>
        <FormField label="Renovação">
          <Select value={form.renovacao} onChange={(e) => upd("renovacao", e.target.value as never)}>
            {RENOVACAO_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </Select>
        </FormField>
        <FormField label="Reajuste">
          <Select value={form.reajuste} onChange={(e) => upd("reajuste", e.target.value as never)}>
            {(API_MODE === "real" ? REAJUSTE_OPTIONS_REAL : REAJUSTE_OPTIONS).map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </Select>
        </FormField>
        <FormField label="Reajuste anual (%)" hint="Opcional. Percentual aplicado ao índice escolhido.">
          <Input
            type="number"
            value={form.reajusteAnualPercentual ?? ""}
            onChange={(e) =>
              upd("reajusteAnualPercentual", e.target.value ? Number(e.target.value) : undefined)
            }
          />
        </FormField>
        {API_MODE === "mock" && (
          <FormField label="Índice de reajuste (texto)">
            <Input
              value={form.indiceReajuste}
              onChange={(e) => upd("indiceReajuste", e.target.value)}
              placeholder="Ex.: 100% do IPCA"
            />
          </FormField>
        )}
        <FormField label="Aviso prévio (dias)">
          <Input
            type="number"
            value={form.avisoPrevioDias ?? ""}
            onChange={(e) => upd("avisoPrevioDias", e.target.value ? Number(e.target.value) : undefined)}
          />
        </FormField>
      </FormSection>

      <FormSection number={4} title="Assinatura (informações)">
        <FormField label="Status da assinatura">
          <Select
            value={form.assinaturaStatus}
            onChange={(e) => upd("assinaturaStatus", e.target.value as never)}
            disabled={API_MODE === "real"}
          >
            <option value="PENDENTE">Pendente</option>
            <option value="ASSINADO">Assinado</option>
            <option value="NAO_APLICAVEL">Não aplicável</option>
          </Select>
        </FormField>
      </FormSection>

      {!isEdit && (
        <RecaptchaCheckbox
          key={recaptchaResetKey}
          onTokenChange={handleRecaptchaToken}
        />
      )}
      {submitError && (
        <p className="text-sm text-red-600" role="alert">
          {submitError}
        </p>
      )}

      <div className="flex justify-end gap-2 pt-2">
        <Button variant="outline" onClick={handleCancel}>
          Cancelar
        </Button>
        <Button
          onClick={handleSubmit}
          loading={submitting}
          disabled={submitting || (!isEdit && !recaptchaToken)}
        >
          {isEdit ? "Salvar alterações" : "Criar contrato"}
        </Button>
      </div>

      {/* Confirmação de cancelamento com alterações não salvas */}
      <Modal
        open={confirmCancel}
        onClose={() => setConfirmCancel(false)}
        title="Descartar alterações?"
        size="sm"
        footer={
          <>
            <Button variant="outline" onClick={() => setConfirmCancel(false)}>
              Continuar editando
            </Button>
            <Button variant="danger" onClick={() => { setConfirmCancel(false); onCancel(); }}>
              Descartar
            </Button>
          </>
        }
      >
        <p className="text-sm text-gray-600">
          Existem alterações não salvas neste formulário. Deseja realmente descartá-las?
        </p>
      </Modal>
    </div>
  );
}

// helper local para carregar cliente
async function clientesObter(id: string): Promise<Cliente | null> {
  return clientes.obterPorId(id);
}
