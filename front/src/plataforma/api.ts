import { avisarTokenExpirado, definirSessaoDireta, sessaoAtual, type Usuario } from './sessao'

/** Envelope de toda resposta da plataforma (Contrato §8.2). */
export interface Envelope<T> {
  success: boolean
  data: T | null
  message: string | null
  errors: { campo: string; codigo: string; detalhe: string }[]
}

export interface Pagina<T> {
  itens: T[]
  pagina: number
  tamanho: number
  total: number
}

export class ErroDaApi extends Error {
  readonly status: number
  readonly envelope: Envelope<unknown> | null

  constructor(status: number, envelope: Envelope<unknown> | null) {
    super(envelope?.message ?? `A requisição falhou (HTTP ${status}).`)
    this.status = status
    this.envelope = envelope
  }
}

export async function chamar<T>(caminho: string, opcoes: RequestInit = {}): Promise<T> {
  const cabecalhos = new Headers(opcoes.headers)
  const sessao = sessaoAtual()
  if (sessao) cabecalhos.set('Authorization', `Bearer ${sessao.token}`)
  if (opcoes.body && !cabecalhos.has('Content-Type')) cabecalhos.set('Content-Type', 'application/json')

  const resposta = await fetch(caminho, { ...opcoes, headers: cabecalhos })
  const envelope = (await resposta.json().catch(() => null)) as Envelope<T> | null

  if (resposta.status === 401) avisarTokenExpirado() // a casca renova e reenvia o token
  if (!resposta.ok || !envelope?.success) throw new ErroDaApi(resposta.status, envelope)
  return envelope.data as T
}

/** Só em modo direto. Embutido, quem faz login é a casca. */
export async function entrarComUsuarioDeTeste(email: string, senha: string) {
  const dados = await chamar<{ accessToken: string; usuario: Usuario & { tenantId: string } }>(
    '/api/identity/auth/login',
    { method: 'POST', body: JSON.stringify({ email, senha }), credentials: 'include' },
  )
  definirSessaoDireta({
    token: dados.accessToken,
    tenantId: dados.usuario.tenantId,
    usuario: dados.usuario,
    tema: 'claro',
  })
}
