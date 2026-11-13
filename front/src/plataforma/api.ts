import { definirSessaoDireta, estaEmbutido, type Usuario } from './sessao'

interface RespostaLogin {
  success: boolean
  data: { accessToken: string; usuario: Usuario & { tenantId: string } } | null
  message: string | null
}

/**
 * Só em modo direto (desenvolvimento): obtém um token REAL do identity de desenvolvimento,
 * com um usuário de teste (Contrato §12 e §14.3). Passa pelo gateway, como em produção.
 * Embutido no iframe, quem faz login é a casca — esta função não faz nada.
 * O token fica só em memória; o módulo nunca renova token (Contrato §4.3).
 */
export async function entrarComUsuarioDeTeste(email: string, senha: string) {
  if (estaEmbutido()) throw new Error('Dentro da plataforma, a sessão vem da casca.')

  const resposta = await fetch('/api/identity/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ email, senha }),
  })
  const corpo = (await resposta.json().catch(() => null)) as RespostaLogin | null
  if (!resposta.ok || !corpo?.success || !corpo.data) {
    throw new Error(corpo?.message ?? `Não foi possível entrar (HTTP ${resposta.status}).`)
  }

  definirSessaoDireta({
    token: corpo.data.accessToken,
    tenantId: corpo.data.usuario.tenantId,
    usuario: corpo.data.usuario,
    tema: 'claro',
  })
}
