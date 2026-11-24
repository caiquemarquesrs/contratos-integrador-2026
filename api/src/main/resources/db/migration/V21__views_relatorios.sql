-- Views públicas para o motor de relatórios do Grupo 3 (Contrato de Integração §9.8, decisão D9).
-- Somente leitura. O consumidor filtra tenant_id pelo token do usuário.

-- Uma linha por contrato: status, valores e vigência, para MRR, contratos a vencer e renovações.
CREATE VIEW contratos.vw_pub_contratos AS
  SELECT id,
         tenant_id,
         numero,
         empresa_id,
         status,
         valor_mensal,
         mrr,
         data_inicio,
         vigencia_meses,
         vigencia_fim,
         renovacao,
         indice_reajuste,
         aviso_previo_dias,
         responsavel_id,
         assinado_em,
         created_at
    FROM contratos.contratos
   WHERE deleted_at IS NULL;

-- Uma linha por serviço contratado, para cruzar com o catálogo de Produtos (vendas por item).
CREATE VIEW contratos.vw_pub_servicos_contratados AS
  SELECT s.id,
         s.tenant_id,
         s.contrato_id,
         s.item_catalogo_id,
         s.descricao,
         s.quantidade,
         s.valor_unitario,
         s.valor_total,
         s.created_at
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
