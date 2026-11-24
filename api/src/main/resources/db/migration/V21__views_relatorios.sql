-- Views públicas para relatórios (Contrato de Integração §9.8), documentadas em
-- infra-integrador-2026/contratos/contratos.views.md. Roda como own_contratos.
-- Faixa de migrations dos documentos/relatórios: V20 a V29.
--
-- Somente leitura e sem os registros excluídos. Quem lê filtra por tenant_id com o valor do token.
-- Não trazem empresa_nome (cópia local): o nome oficial vem de crm.vw_pub_empresas.

CREATE VIEW contratos.vw_pub_contratos AS
  SELECT id, tenant_id, numero, empresa_id, status, valor_mensal, mrr, data_inicio,
         vigencia_meses, vigencia_fim, renovacao, indice_reajuste, aviso_previo_dias,
         responsavel_id, assinado_em, created_at
    FROM contratos.contratos
   WHERE deleted_at IS NULL;

CREATE VIEW contratos.vw_pub_servicos_contratados AS
  SELECT s.id, s.tenant_id, s.contrato_id, s.item_catalogo_id, s.descricao, s.quantidade,
         s.valor_unitario, s.valor_total, s.created_at
    FROM contratos.servicos_contratados s
    JOIN contratos.contratos c ON c.id = s.contrato_id
   WHERE s.deleted_at IS NULL
     AND c.deleted_at IS NULL;

-- O IF EXISTS deixa a migration rodar nos testes, onde usr_chamados não existe.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'usr_chamados') THEN
    GRANT USAGE  ON SCHEMA contratos                      TO usr_chamados;
    GRANT SELECT ON contratos.vw_pub_contratos            TO usr_chamados;
    GRANT SELECT ON contratos.vw_pub_servicos_contratados TO usr_chamados;
  END IF;
END $$;
