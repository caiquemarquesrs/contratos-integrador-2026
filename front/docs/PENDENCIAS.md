# Pendências — Módulo Contratos e Documentos

> Atualizado em 2026-11-09 após auditoria de integração. O modo mock segue implementado; as limitações do modo real estão discriminadas abaixo.

| # | Tela/área afetada | Pendência | Solução provisória adotada | Quem decide |
|---|---|---|---|---|
| 1 | Todo o projeto | Repositório `Front1.0.0.1` está vazio — não há CRM existente para integrar | Scaffolding base criado conforme o design system (React 19 + TS + Vite + Tailwind v4 + componentes UI) | Usuário |
| 2 | Seleção de cliente (7.8) | Auditoria confirmou que a API não possui controller/endpoints para listar ou consultar clientes; o CRM é consultado somente pelo back-end durante o cadastro | Manter os dados apenas no mock; no modo real, informar a indisponibilidade sem usar dados fictícios ou inventar rotas | Grupo clientes / back-end |
| 3 | Status (8) | Enum coincide com a API, mas não há operação para todas as transições oferecidas pela interface | No modo real, permitir apenas `EM_APROVACAO → AGUARDANDO_ASSINATURA` e `AGUARDANDO_ASSINATURA → ATIVO`; manter simulação completa no mock | Grupo back-end |
| 4 | Status (8) | A API não expõe `RASCUNHO → EM_APROVACAO` nem alteração genérica de status | Usar os endpoints reais de aprovação/assinatura; demais transições ficam pendentes e não são simuladas no modo real | Grupo back-end |
| 5 | Status (8) | "A VENCER" e status derivados de data — quem calcula? | Não calculado no front-end; exibido conforme retornado pela API | Grupo back-end |
| 6 | Cadastro (7.2) | Mock não calcula MRR; API real exige MRR obrigatório | Preservar mock; cadastro real bloqueado até alinhar campo e regra de preenchimento | Grupo front-end / back-end |
| 7 | Cadastro (7.2) | Mock solicita o número, mas a API gera esse valor | Preservar comportamento fake; cadastro real bloqueado até formulário refletir criação e retorno da API | Grupo front-end |
| 8 | Modelos (RF-03) | Geração automática de documentos a partir de modelos | Implementar apenas seleção de modelo existente; geração automática fora do escopo | Grupo back-end |
| 9 | Documentos (7.5, 7.6) | Upload confirmado como multipart com campo `arquivo`; API aceita `nome` opcional e limita tamanho a 5 MB, sem categoria ou descrição | Serviço real envia `FormData`; categoria/descrição da tela não são persistidas até contrato do back-end defini-las | Grupo back-end |
| 10 | Permissões (RF-09) | Não existe sistema de permissões no repositório | Pontos de ação preparados para receber checagem; sem criar sistema de permissões | Grupo back-end |
| 11 | Todas as telas | Arquivo `descricao-telas.txt` referenciado no message.txt não foi fornecido | Campos deduzidos do RF-02 e da seção 7 do message.txt | Usuário |
| 12 | Edição (7.4) | API real só permite editar contratos em `RASCUNHO` e `EM_APROVACAO`; demais diferenças de DTO também impedem gravação | Edição real bloqueada até alinhar os DTOs; mock mantém o comportamento existente | Grupo front-end / back-end |
| 13 | Build | O front usa Vite 6 em vez do Vite 8 especificado na stack de referência | Manter a versão atual nesta etapa, sem alterações de dependências fora do escopo de integração | Usuário / infra |
| 14 | Build | Tailwind Oxide não instala binding nativo via optional dependencies (bug do npm) | `@tailwindcss/oxide-win32-x64-msvc` adicionado como `devDependency` direta | Usuário / infra |
| 15 | Build | ESLint não instalado nem configurado no repositório | Script `lint` removido do `package.json`; validação por `tsc --noEmit` e `build` | Usuário |
| 16 | Documentos (7.5) | Mock não persiste conteúdo de arquivo | Mock continua exibindo apenas metadados; modo real baixa o binário pelo endpoint existente | Grupo back-end |
| 17 | Status (7.3) | Ação "Alterar Status" dedicada no painel de detalhes | Mock mantém transições simuladas; real oferece apenas aprovação/assinatura nos estados aceitos pela API | Grupo back-end |
| 18 | Cadastro/edição real | A tela e os DTOs da API diferem: valor total vs. valor mensal, MRR opcional vs. obrigatório, serviços sem quantidade, responsável textual vs. UUID; cadastro também exige `origem` e `origemModuloId` | Gravações reais são bloqueadas com erro explícito para evitar persistência incorreta. Alinhar campos/semântica antes de habilitar | Grupo front-end / back-end |
| 19 | CORS e autenticação | API exige JWT/permissões; não foi localizada configuração CORS; front de desenvolvimento e API usam portas/origens distintas | Receber token da plataforma e configurar CORS/gateway na infraestrutura; sem credenciais inventadas | Grupo plataforma / back-end |
| 20 | Histórico e documentos | DTO de histórico não inclui nome do usuário; DTO de documento não inclui categoria/descrição | Interface usa UUID e MIME retornados pela API; definir campos adicionais se produto exigir nomes/categorias | Grupo back-end |
| 21 | Reajuste de contrato | Tela oferece `SELIC`/`OUTRO` e não coleta percentual; API aceita `IPCA`/`IGPM`/`INPC`/`nenhum` e percentual anual | Alinhar enumeração e campo percentual antes de habilitar cadastro/edição real | Grupo front-end / back-end |

## Atualização da etapa de finalização

- O modo real continua bloqueando cadastro e edição. O formulário explica os dados ausentes, o botão de gravação fica desabilitado e o serviço mantém a proteção contra chamada direta.
- Os detalhes/tabela distinguem `valorMensal` do valor total do mock. A API não retorna nome do responsável; a interface exibe essa ausência, não o UUID como nome. Histórico mostra o identificador com rótulo explícito.
- Status só oferece aprovação e assinatura nos estados correspondentes. Categorias e descrições dos documentos continuam preenchíveis no modo real, com aviso explícito de que a API não as persiste.
- CORS permanece dependência externa: autorizar a origem efetivamente usada (`http://localhost:5173` no Vite ou `http://localhost:3004` no container) no gateway/API.
- O gateway também precisa expor `Content-Disposition` para o navegador permitir que o front use o nome do arquivo baixado.
- `npm run typecheck`, `npm run build`, smoke test dos serviços mock e exercício dos adaptadores reais com HTTP simulado passaram. O servidor real em `localhost:8084` não estava acessível; portanto a integração real em runtime continua pendente.

## Integração com a casca (iframe) — 2026-11-13

| # | Área | Pendência | Situação | Quem decide/valida |
|---|---|---|---|---|
| 22 | Integração | Validar na casca real (staging ou `docker compose` do infra): handshake, sessão, tema, renovação de token, `modulo:altura`, `modulo:navegar` e `modulo:notificar` | Testado só com casca simulada na mesma origem (Chrome headless) | Grupo 2 / Grupo 5 |
| 23 | Navegação | Submenus `/modelos` e `/documentos` do `modulos/contratos.json` não têm tela própria | Mostram aviso e link para Contratos; documentos ficam no detalhe do contrato | Grupo 5 |
| 24 | Navegação | Comportamento do botão voltar depende de como a casca trata `modulo:navegar` | Embutido, o módulo usa `replaceState` e deixa o histórico com a casca | Grupo 2 |
| 25 | Tema | Componentes usam cores fixas (`bg-white`, `text-slate-*`); o tema escuro só muda fundo/texto base | `data-tema` é aplicado; migrar para os tokens `fundo`/`superficie`/`texto` do tema oficial | Grupo 5 |
| 26 | Docker | Imagem do front e cabeçalhos do nginx não foram testados em container nesta etapa | Configuração conferida por leitura (`frame-ancestors 'self'`, sem `DENY`) | Grupo 5 |
