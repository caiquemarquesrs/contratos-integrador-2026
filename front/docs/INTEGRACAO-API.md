# Auditoria da integração com a API

> **Atualização — integração com a casca (iframe).** As seções abaixo descrevem a auditoria
> anterior. O que mudou na adequação ao Contrato de Integração v0.7
> (`infra-integrador-2026/docs/contrato-de-integracao.md`, §3, §4, §8.2, §8.6, §9.1, §12.1, §12.2 e §12.8):
>
> | Antes | Agora |
> |---|---|
> | Modo padrão `mock`; a imagem Docker publicava dados simulados | Padrão `real`. `VITE_API_MODE=mock` só vale em modo direto; dentro do iframe é sempre a API real |
> | `VITE_API_URL=http://localhost:8084/api` (outra origem, quebrava no iframe) | `/api`, na mesma origem do gateway (`/api/contratos/**`) |
> | Vite: `/api` → `:8084`, `/api/identity` → `:8080` | Vite: todo `/api` → gateway (`VITE_GATEWAY`, padrão `http://localhost:8080`) |
> | `iniciarSessao()` nunca era chamado: sem ouvinte, sem `modulo:pronto`, sem sessão | Chamado em `main.tsx` antes do render: ouvinte registrado **antes** de `modulo:pronto` |
> | Sidebar, logotipo e cabeçalho global próprios em `App.tsx` | Removidos: menu, barra e login são da casca |
> | Sem rotas; `/contratos/{id}` da busca global não abria nada | Rotas internas `/contratos` e `/contratos/{id}`, com `modulo:navegar` |
> | `401` só avisava a casca | `401` → `modulo:token-expirado` (uma vez), espera `plataforma:token` e repete a chamada uma vez |
> | `html/body/#root` com `height:100%`/`100vh` e rolagem interna | Altura do conteúdo, informada em `modulo:altura` (sobe e desce) |
>
> O protocolo completo está em [Integração com a casca](#integração-com-a-casca-iframe), no fim deste arquivo.

Auditoria realizada sobre os controllers e DTOs atuais de `api/` e os serviços de `front/`. O front-end não altera o contrato nem o código Java. A API fake continua sendo o modo padrão.

## Configuração e autenticação

| Configuração | Valor verificado |
|---|---|
| Modo padrão | `VITE_API_MODE=mock` |
| URL base real | `VITE_API_URL=http://localhost:8084/api` |
| API | Porta `8084` por padrão em `api/src/main/resources/application.yml` |
| Front em desenvolvimento | Vite na porta `5173` (`front/vite.config.ts`) |
| Front em container | Nginx na porta `3004` (`front/Dockerfile`) |

Para alternar, configure `front/.env` (ou `.env.local`) e reinicie o Vite:

```env
VITE_API_MODE=mock
VITE_API_URL=http://localhost:8084/api
```

O mock é o padrão e não realiza requisições HTTP. Para selecionar os serviços reais, altere apenas `VITE_API_MODE=real`. Não configure credenciais no arquivo; a sessão/token deve vir da plataforma.

O cliente HTTP remove a barra final da URL base, envia o token Bearer recebido pela sessão da plataforma e converte o envelope `Resposta<T>` e os erros `ErroCampo` para os tipos consumidos pelo front. O endpoint da API exige JWT e permissões; a API de contratos não oferece login. Sem uma sessão válida, respostas `401` são encaminhadas à casca da plataforma. O helper `src/plataforma/api.ts` aponta para login do serviço identity e não é utilizado pelos componentes atuais; credenciais não são adicionadas ao módulo de contratos.

Não foi encontrada configuração CORS em `api/`. O Vite e o Nginx deste módulo também não fazem proxy de API. Portanto, acesso local entre `http://localhost:5173` (Vite) ou `http://localhost:3004` (container) e `http://localhost:8084` depende de CORS ou gateway configurado pelo grupo responsável pelo back-end/infraestrutura. A origem efetivamente utilizada pelo navegador deve ser autorizada; o gateway também deve expor o cabeçalho `Content-Disposition` para o nome original do arquivo baixado ficar disponível ao front. Essa integração de rede não foi validada aqui.

## Matriz de integração verificada

| Funcionalidade | Endpoint do front | Endpoint real da API | Diferença encontrada | Situação/correção |
|---|---|---|---|---|
| Listagem de contratos | `GET /contratos` | `GET /api/contratos/contratos` | Rota, parâmetros e paginação diferentes | Alinhado: `pagina` (0-based), `tamanho`, `q`, `empresaId`, `status`; resposta paginada convertida para `items/page/pageSize/total`. Busca real exige ao menos 2 caracteres. |
| Cadastro de contratos | `POST /contratos` | `POST /api/contratos/contratos` | A API exige `origem`, `origemModuloId`, `empresaId`, `responsavelId`, MRR e serviços detalhados; a tela não tem todos esses dados. O índice de reajuste também difere (`SELIC`/`OUTRO` na tela; `INPC`/`nenhum` na API). A API gera número/status e chama o CRM internamente. | Bloqueado com erro explícito no modo real; nenhum valor de origem, ID, responsável ou regra comercial é inventado. |
| Detalhes de contratos | `GET /contratos/{id}` | `GET /api/contratos/contratos/{id}` | DTO usa `empresaId`, `empresaNome`, `servicosContratados`, `valorMensal` etc. | Rota alinhada e DTO normalizado para o tipo de tela. A tela chama `valorMensal` de “Valor total” e mostra o UUID de `responsavelId` como texto; sem alinhamento semântico/nome, esses campos não são equivalentes. |
| Edição de contratos | `PUT /contratos/{id}` | `PUT /api/contratos/contratos/{id}` | O DTO da API não aceita cliente, número ou status, e requer campos/semântica que a tela não coleta (responsável UUID, quantidade e valor mensal). A API também aceita `INPC`/`nenhum` e reajuste percentual; a tela oferece índices diferentes e não captura percentual. | Bloqueado com erro explícito no modo real para evitar atualizar dados com mapeamento incorreto. A API só permite editar estados `RASCUNHO` e `EM_APROVACAO`. |
| Alteração de status | `PATCH /contratos/{id}/status` | `POST /api/contratos/contratos/{id}/aprovar` e `POST /api/contratos/contratos/{id}/assinar` | A API não tem PATCH genérico; aprovar e assinar são operações distintas. | Alinhado apenas para `EM_APROVACAO → AGUARDANDO_ASSINATURA` e `AGUARDANDO_ASSINATURA → ATIVO`. Assinatura usa o UUID do usuário autenticado. Outras transições ficam indisponíveis no modo real. |
| Modelos de contrato | `GET /modelos-contrato` | `GET /api/contratos/modelos` | Rota e resposta paginada/envelopada diferentes. | Alinhado; páginas de até 100 itens são agregadas e DTO normalizado. Não há endpoint para gerar documento a partir do modelo. |
| Listagem de documentos | `GET /contratos/{id}/documentos` | `GET /api/contratos/contratos/{id}/documentos` | Resposta paginada/envelopada; metadados têm nomes diferentes. | Alinhado; páginas agregadas e `DocumentoDto` normalizado. O front exibe `tipoArquivo` no campo de tipo/categoria. |
| Upload de documentos | `POST /contratos/{id}/documentos` | `POST /api/contratos/contratos/{id}/documentos` | API exige `multipart/form-data` com campo `arquivo`; aceita `nome` opcional. A tela também possui categoria e descrição, que a API não recebe. | Alinhado: envia `FormData` sem definir manualmente `Content-Type`, exige arquivo no modo real e limita a 5 MB. Categoria/descrição não são persistidas pela API. |
| Download de documentos | Exibia somente modal com metadados | `GET /api/contratos/documentos/{id}/download` | API retorna bytes (fora do envelope) com `Content-Disposition`. | Alinhado no modo real: baixa blob e usa o nome informado pelo servidor. O mock continua informativo e não armazena conteúdo. |
| Exclusão de documentos | `DELETE /documentos/{id}` | `DELETE /api/contratos/documentos/{id}` | Rota-base era incompleta; API responde com envelope. | Alinhado. |
| Clientes/empresas | `GET /clientes`, `GET /clientes/{id}` | Não existe controller/endpoints de clientes. | O CRM só é consultado internamente pela API ao criar um contrato; não há busca/listagem de clientes no contrato atual. | Mantido apenas no mock. No modo real, o front informa explicitamente a indisponibilidade, sem usar clientes fictícios nem chamar endpoints inventados. |
| Histórico | `GET /contratos/{id}/historico` | `GET /api/contratos/contratos/{id}/historico` | Resposta paginada/envelopada; campos `campo`, `usuarioId`, `ocorridoEm`. | Alinhado; páginas agregadas e campos mapeados para o formato da interface. Nome do usuário não existe no DTO, portanto é exibido o UUID. |

## Diferenças de contrato que exigem decisão do grupo responsável

1. **Cadastro/edição**: a tela tem “Valor total”, MRR opcional, serviços sem quantidade, responsável em texto e cliente, mas a API trabalha com `valorMensal`, MRR obrigatório, serviços com quantidade/valores, `responsavelId` UUID e, no cadastro, `origem` e `origemModuloId`. A tela não captura reajuste percentual; oferece `SELIC`/`OUTRO`, enquanto a API aceita `IPCA`/`IGPM`/`INPC`/`nenhum`. A API também gera `numero` e começa em `RASCUNHO`; não aceita status enviado pela tela. É necessário alinhar os campos e a semântica antes de habilitar gravação real.
2. **Clientes/CRM**: falta um endpoint para consultar empresas/clientes e fornecer seus UUIDs ao formulário. Não reutilizar `/api/contratos/busca`, que pesquisa contratos e não clientes.
3. **Status**: não existe operação `RASCUNHO → EM_APROVACAO` nem alterações genéricas para suspender, cancelar, renovar ou finalizar. A API atualmente implementa aprovação e assinatura apenas.
4. **Documento**: API não recebe `tipo` como categoria ou `descricao`; somente arquivo e nome opcional. Confirmar se a categoria precisa ser adicionada ao contrato antes de persistir esse dado.
5. **Autenticação e rede**: a API espera JWT com permissões, e a origem do front precisa ser autorizada por CORS/gateway. A emissão do token e as permissões devem vir da plataforma/identity.
6. **Modelos**: existe listagem e consulta/criação administrativa, mas não geração de documentos a partir do modelo.

## Respostas e erros

Respostas JSON seguem `{ success, data, message, errors }`; páginas seguem `{ itens, pagina, tamanho, total }`, com índice de página começando em zero. O front normaliza esses formatos sem expor o envelope aos componentes. Erros `{ campo, codigo, detalhe }` são convertidos em mensagens e erros por campo. O download é a única rota conferida que retorna conteúdo binário fora do envelope.

## Atualização — etapa de finalização

- Cadastro e edição permanecem bloqueados no modo real; o formulário mostra os campos de contrato que precisam ser alinhados, e o serviço também impede a gravação caso seja chamado diretamente.
- Detalhes e tabela distinguem `valorMensal` do valor total do mock, exibem o percentual anual de reajuste retornado pela API e não apresentam UUID como nome de responsável. O histórico identifica o campo como ID do usuário, sem inventar um nome.
- Aprovação/assinatura exibem somente as transições com operação real; para outros estados, a interface explica que não há operação disponível.
- Upload real valida arquivo obrigatório e limite de 5 MB na interface e no serviço. Categoria e descrição continuam disponíveis, mas a interface informa que a API atual não as persiste.
- Erros estruturados, conexão indisponível e permissões são reportados por mensagens explícitas. O modo padrão continua sendo mock.

## Verificação desta etapa

- `npm run typecheck`: passou.
- `npm run build`: passou (Vite `6.4.4`).
- Exercício dos serviços mock: passou para seleção padrão, listagem/pesquisa/paginação, clientes/modelos, cadastro/edição/status mock, documentos (metadados e exclusão) e histórico. A seleção default foi testada com `fetch` que falha, confirmando que não chama a API.
- Exercício dos adaptadores reais com respostas HTTP simuladas: passou para contratos, modelos, histórico, documentos, upload multipart, download, status, autenticação Bearer e falha de rede. Não substitui teste contra servidor real.
- Testes automatizados do front: não há script/test runner configurado em `package.json`; os exercícios acima foram smoke tests executados localmente.
- Disponibilidade real: `GET http://localhost:8084/api/contratos/health` não conectou (servidor indisponível). Integração real em runtime continua não validada; depende de API disponível, token/permissões, CORS/gateway e resolução das diferenças listadas.
- `npm ci` também reportou uma vulnerabilidade moderada em Vite `6.4.4` (GHSA-8g5g-mxqh-grgj): `server.fs.deny` pode ser contornado com query string no servidor de desenvolvimento. Há correção disponível; `6.4.6` está dentro da faixa `^6.0.0` e suporta o Node instalado (`24.14.1`). A dependência não foi atualizada nesta etapa e `npm audit fix` não foi executado.

## Google reCAPTCHA v2 no cadastro

- O formulário de novo contrato usa o checkbox v2 e só habilita o botão após receber um token. Expiração, falha de carregamento e rejeição da API invalidam o token e exigem uma nova verificação.
- A Site Key do navegador é `VITE_RECAPTCHA_SITE_KEY`; configure-a localmente em `front/.env` com o prefixo `VITE_`. A chave fica pública no bundle.
- A API recebe o token pelo cabeçalho `X-Recaptcha-Token` no `POST /api/contratos/contratos` e exige `success=true` do Google antes da consulta ao CRM e da persistência. `RECAPTCHA_SECRET_KEY` é lida somente pela API; sem ela, ou se o Google estiver indisponível, a criação falha sem persistir.
- Para obter as chaves, crie no Google reCAPTCHA Admin Console um registro **reCAPTCHA v2 > Checkbox** e autorize o domínio `localhost` (sem porta). Configure a Site Key no front e a Secret Key como variável de ambiente da API. Não versionar nenhuma chave real.
- A gravação real pelo formulário permanece bloqueada pelo desalinhamento de contrato já descrito na matriz acima. O CAPTCHA não remove essa proteção nem inventa campos de negócio; para o POST do front, a integração só poderá ser ligada após a decisão sobre os dados ausentes.

## Integração com a casca (iframe)

Referência: `infra-integrador-2026/docs/contrato-de-integracao.md` §12 e `exemplo-modulo/front/src/plataforma/sessao.ts`.
Caminhos oficiais (`infra-integrador-2026/modulos/contratos.json`): front em `/modulos/contratos/`, API em `/api/contratos`.

### Onde está cada coisa

| Arquivo | Responsabilidade |
|---|---|
| `src/plataforma/sessao.ts` | Único ponto que distingue modo direto × embutido. Ouvinte de `message`, validação de `event.origin` (= `window.location.origin`) e `event.source` (= `window.parent`), validação do formato das mensagens, sessão em memória fora do React, tema, altura, `modulo:*` |
| `src/plataforma/rotas.ts` | Rotas internas relativas ao `urlFrontend` (`/contratos`, `/contratos/{id}`); envia `modulo:navegar` |
| `src/plataforma/api.ts` | Login com usuário de teste — **só no modo direto** (Contrato §12 e §14.3) |
| `src/main.tsx` | `iniciarSessao()` e `iniciarRotas()` antes do primeiro render |
| `src/App.tsx` | Sem sessão: “Aguardando a sessão da plataforma” (embutido) ou login de teste (direto). Com sessão: as telas |
| `src/services/real/http.ts` | `Authorization: Bearer` com o token atual; fluxo de `401` |

### Ciclo de mensagens

1. `main.tsx` → `iniciarSessao()`: registra o ouvinte, liga o `ResizeObserver` e **só então** envia `{ tipo: 'modulo:pronto' }` para `window.parent` com `targetOrigin = window.location.origin` (nunca `'*'`). Em modo direto nada é enviado.
2. `plataforma:sessao` → guarda `token`, `tenantId`, `usuario` e `tema` em memória e aplica `data-tema` no `<html>`. Mensagem com formato inválido, de outra origem ou de outra janela é ignorada.
3. `plataforma:tema` → atualiza `data-tema`. `plataforma:token` → troca o token usado pelo cliente HTTP.
4. `401` da API → `modulo:token-expirado` (uma vez, mesmo com várias chamadas simultâneas), espera até 10 s por um token novo e repete a chamada uma vez. O módulo nunca renova token nem mostra login dentro da casca.
5. `modulo:altura` → altura do `body` a cada mudança de conteúdo (ResizeObserver + `requestAnimationFrame`, sem repetir valor igual).
6. `modulo:navegar` → rota relativa sem o código (`/contratos/9f1c…`). Embutido, o módulo usa `replaceState` e deixa o histórico com a casca; em modo direto usa `pushState`.
7. `modulo:notificar` → enviado ao criar contrato. Mensagens de validação dos formulários continuam na própria tela.

Nada da sessão é gravado em `localStorage`, `sessionStorage` ou cookie. O mock (só em modo direto, opt-in) ainda guarda **dados simulados** em `localStorage`, nunca token.

### Modo direto (desenvolvimento)

```bash
cp .env.example .env    # VITE_API_MODE=real, VITE_GATEWAY=http://localhost:8080
npm run dev             # http://localhost:5173/modulos/contratos/
```

Com o `docker compose` do `infra-integrador-2026` no ar, entre com um usuário de `docs/usuarios-de-teste.md`; o token real do identity de desenvolvimento fica só em memória. Para desenhar tela sem back-end, `VITE_API_MODE=mock` dispensa login.

### Servidor e iframe

`nginx/default.conf.template` serve o `dist/` em `/modulos/contratos/`, com fallback de SPA para `index.html` (recarregar `/modulos/contratos/contratos/{id}` funciona) e `Content-Security-Policy: frame-ancestors 'self'`. Não há `X-Frame-Options: DENY` nem `frame-ancestors 'none'`; o `X-Frame-Options: SAMEORIGIN` vem do gateway (Contrato §12.8).
