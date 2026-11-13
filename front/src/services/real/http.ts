import { aguardarTokenRenovado, sessaoAtual } from "@/plataforma/sessao";
import type { ApiError } from "@/types/contratos";

interface ApiEnvelope<T> {
  success: boolean;
  data: T | null;
  message: string | null;
  errors: { campo: string; codigo: string; detalhe: string }[];
}

export interface ApiPage<T> {
  itens: T[];
  pagina: number;
  tamanho: number;
  total: number;
}

/**
 * Caminho relativo à própria origem: o navegador só conhece o gateway, que encaminha
 * /api/contratos/** para a API do módulo (Contrato §3, §9.1 e §12.8). Uma URL absoluta para
 * outra porta (ex.: localhost:8084) quebra dentro da casca: outra origem, CORS e sem gateway.
 */
const BASE_URL = (import.meta.env.VITE_API_URL || "/api").replace(/\/+$/, "");

/**
 * Faz a chamada com o token atual. Num 401, avisa a casca (modulo:token-expirado), espera o
 * token renovado e repete UMA vez. O módulo nunca renova token sozinho (Contrato §4.3).
 */
async function comToken(
  path: string,
  init: RequestInit,
): Promise<Response> {
  const enviar = (token: string | undefined) => {
    const headers = new Headers(init.headers);
    if (token) headers.set("Authorization", `Bearer ${token}`);
    return fetchApi(`${BASE_URL}${path}`, { ...init, headers });
  };

  const tokenUsado = sessaoAtual()?.token;
  const response = await enviar(tokenUsado);
  if (response.status !== 401) return response;

  const renovado = await aguardarTokenRenovado(tokenUsado);
  return renovado ? enviar(sessaoAtual()?.token) : response;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isEnvelope<T>(value: unknown): value is ApiEnvelope<T> {
  return (
    isRecord(value) &&
    typeof value.success === "boolean" &&
    "data" in value &&
    (value.message === null || typeof value.message === "string") &&
    Array.isArray(value.errors) &&
    value.errors.every(
      (error) =>
        isRecord(error) &&
        typeof error.campo === "string" &&
        typeof error.codigo === "string" &&
        typeof error.detalhe === "string",
    )
  );
}

function toApiError(status: number, body: unknown): ApiError {
  const envelope = isEnvelope<unknown>(body) ? body : null;
  const fieldErrors: Record<string, string> = {};
  for (const error of envelope?.errors ?? []) {
    if (error.campo) fieldErrors[error.campo] = error.detalhe;
  }

  const message =
    envelope?.message ||
    (envelope?.errors ?? [])
      .map((error) => error.detalhe)
      .filter(Boolean)
      .join(" ") ||
    (status === 401
      ? "Sessão expirada ou não autenticada."
      : status === 403
        ? "Você não tem permissão para executar esta operação."
        : `Erro ${status} na requisição.`);

  return {
    code: envelope?.errors[0]?.codigo ?? `HTTP_${status}`,
    message,
    ...(Object.keys(fieldErrors).length > 0 ? { fieldErrors } : {}),
  };
}

async function fetchApi(
  input: RequestInfo | URL,
  init?: RequestInit,
): Promise<Response> {
  try {
    return await fetch(input, init);
  } catch (error) {
    if (error instanceof TypeError) {
      throw {
        code: "NETWORK_ERROR",
        message:
          "Não foi possível conectar à API. Verifique a URL, a conexão, CORS e se o servidor está disponível.",
      } satisfies ApiError;
    }
    throw error;
  }
}

async function readJson(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch (error) {
    if (error instanceof SyntaxError) return null;
    throw error;
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");

  if (
    options.body &&
    !(options.body instanceof FormData) &&
    !headers.has("Content-Type")
  ) {
    headers.set("Content-Type", "application/json");
  }

  const response = await comToken(path, {
    ...options,
    method: options.method ?? "GET",
    headers,
  });

  if (response.status === 204) return undefined as T;

  const body = await readJson(response);

  if (!response.ok) throw toApiError(response.status, body);
  if (!isEnvelope<T>(body)) {
    throw {
      code: "INVALID_API_RESPONSE",
      message: "A API retornou uma resposta fora do envelope esperado.",
    } satisfies ApiError;
  }
  if (!body.success) throw toApiError(response.status, body);
  return body.data as T;
}

async function baixar(
  path: string,
): Promise<{ blob: Blob; filename: string | null }> {
  // Token no cabeçalho, nunca na URL (Contrato §8.2): o arquivo vem como blob pelo fetch.
  const response = await comToken(path, { headers: { Accept: "*/*" } });
  if (!response.ok) {
    throw toApiError(response.status, await readJson(response));
  }

  const disposition = response.headers.get("content-disposition");
  const encodedName = disposition?.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  const plainName = disposition?.match(/filename="?([^";]+)"?/i)?.[1];
  let filename = plainName ?? null;
  if (encodedName) {
    try {
      filename = decodeURIComponent(encodedName);
    } catch (error) {
      if (!(error instanceof URIError)) throw error;
      filename = encodedName;
    }
  }

  return { blob: await response.blob(), filename };
}

export async function listarTodasAsPaginas<T>(
  path: string,
): Promise<T[]> {
  const pageSize = 100;
  const result: T[] = [];
  let pagina = 0;
  let total = Number.POSITIVE_INFINITY;

  while (result.length < total) {
    const query = new URLSearchParams({
      pagina: String(pagina),
      tamanho: String(pageSize),
    });
    const page = await request<ApiPage<T>>(`${path}?${query}`);
    result.push(...page.itens);
    total = page.total;
    pagina += 1;
    if (page.itens.length === 0) break;
  }

  return result;
}

export const http = {
  get: <T>(path: string, signal?: AbortSignal) =>
    request<T>(path, { method: "GET", signal }),
  post: <T>(path: string, body?: unknown, headers?: Record<string, string>) =>
    request<T>(path, {
      method: "POST",
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      ...(headers === undefined ? {} : { headers }),
    }),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: "PUT", body: JSON.stringify(body) }),
  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
  upload: <T>(path: string, body: FormData) =>
    request<T>(path, { method: "POST", body }),
  baixar,
};
