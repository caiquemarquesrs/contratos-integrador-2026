import { useRef, useState } from "react";
import { Button, FormField, FormSection, Input, Select, Textarea } from "@/components/ui";
import { API_MODE, documentos } from "@/services";
import { getErrorMessage, isApiError } from "@/types/contratos";
import type { Documento, DocumentoCreateInput } from "@/types/contratos";

interface DocumentoFormProps {
  contratoId: string;
  onCancel: () => void;
  onSuccess: (doc: Documento) => void;
}

const TIPOS = ["CONTRATO", "ANEXO", "CERTIDAO", "PROPOSTA", "OUTRO"];
const MAX_FILE_SIZE = 5 * 1024 * 1024;

export function DocumentoForm({ contratoId, onCancel, onSuccess }: DocumentoFormProps) {
  const [nome, setNome] = useState("");
  const [tipo, setTipo] = useState("CONTRATO");
  const [descricao, setDescricao] = useState("");
  const [arquivoNome, setArquivoNome] = useState("");
  const [arquivoTamanho, setArquivoTamanho] = useState<number | undefined>();
  const [arquivoTipo, setArquivoTipo] = useState("");
  const [arquivo, setArquivo] = useState<File | undefined>();
  const [error, setError] = useState("");
  const [fileError, setFileError] = useState("");
  const [submissionError, setSubmissionError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (f) {
      setArquivo(f);
      setArquivoNome(f.name);
      setArquivoTamanho(f.size);
      setArquivoTipo(f.type);
      if (!nome) setNome(f.name);
      setFileError(
        API_MODE === "real" && f.size > MAX_FILE_SIZE
          ? "O arquivo passa do tamanho máximo de 5 MB aceito pela API."
          : "",
      );
    }
  };

  const handleSubmit = async () => {
    if (API_MODE === "real" && (!arquivo || arquivo.size > MAX_FILE_SIZE)) {
      setFileError(
        !arquivo
          ? "Selecione o arquivo obrigatório para a API real."
          : "O arquivo passa do tamanho máximo de 5 MB aceito pela API.",
      );
      return;
    }
    if (!nome.trim()) {
      setError("Informe o nome do documento.");
      return;
    }
    setSubmitting(true);
    setSubmissionError("");
    try {
      const input: DocumentoCreateInput = {
        contratoId,
        nome: nome.trim(),
        tipo,
        descricao: descricao.trim() || undefined,
        arquivo,
        arquivoNome: arquivoNome || undefined,
        arquivoTamanho,
        arquivoTipo: arquivoTipo || undefined,
      };
      const doc = await documentos.adicionar(input);
      onSuccess(doc);
    } catch (e) {
      const message = getErrorMessage(e, "Erro ao adicionar documento.");
      if (isApiError(e) && (e.fieldErrors?.arquivo || e.code.startsWith("ARQUIVO_"))) {
        setFileError(e.fieldErrors?.arquivo ?? message);
      } else {
        setSubmissionError(message);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-2">
      <FormSection title="Documento">
        <FormField
          label="Arquivo"
          error={fileError}
          hint={
            API_MODE === "mock"
              ? "No modo simulado, apenas os metadados são armazenados."
              : "Obrigatório no modo real; tamanho máximo de 5 MB."
          }
        >
          <input
            ref={fileRef}
            type="file"
            onChange={handleFile}
            className="block w-full text-sm text-gray-500 file:mr-3 file:rounded-lg file:border-0 file:bg-brand-100 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-brand-800 hover:file:bg-brand-200"
          />
          {arquivoNome && (
            <p className="mt-1 text-xs text-gray-500">
              {arquivoNome} ({((arquivoTamanho ?? 0) / 1024).toFixed(1)} KB)
            </p>
          )}
        </FormField>
        {API_MODE === "real" && (
          <div className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800 md:col-span-2">
            A API ainda não persiste a categoria nem a descrição informadas abaixo. Esses campos
            serão mantidos apenas nesta tela e não serão enviados.
          </div>
        )}
        <FormField label="Nome do documento" required error={error}>
          <Input value={nome} onChange={(e) => { setNome(e.target.value); setError(""); }} />
        </FormField>
        <FormField label="Tipo / categoria">
          <Select value={tipo} onChange={(e) => setTipo(e.target.value)}>
            {TIPOS.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </Select>
        </FormField>
        <FormField label="Descrição" colSpan={2}>
          <Textarea
            rows={3}
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
            placeholder="Descrição opcional..."
          />
        </FormField>
      </FormSection>

      {submissionError && (
        <p role="alert" className="text-sm text-red-600">{submissionError}</p>
      )}

      <div className="flex justify-end gap-2 pt-2">
        <Button variant="outline" onClick={onCancel}>Cancelar</Button>
        <Button onClick={handleSubmit} loading={submitting} disabled={submitting}>
          Adicionar
        </Button>
      </div>
    </div>
  );
}
