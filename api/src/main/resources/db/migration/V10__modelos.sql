-- Tarefa 6: modelos de contrato (RF-03). Roda como own_contratos (Contrato de Integração §7.1).
-- Faixa de migrations desta tarefa: V10 a V19 (o Back B usa V1 a V9).

-- As sete colunas obrigatórias da §7.2: id, tenant_id, created_at, updated_at, deleted_at,
-- created_by e updated_by.
CREATE TABLE contratos.modelos (
  id          uuid PRIMARY KEY,
  tenant_id   uuid NOT NULL,
  nome        varchar(255) NOT NULL,
  descricao   varchar(500),
  conteudo    text NOT NULL,
  created_at  timestamptz NOT NULL,
  updated_at  timestamptz NOT NULL,
  deleted_at  timestamptz,
  created_by  uuid,
  updated_by  uuid
);
CREATE INDEX idx_modelos_tenant ON contratos.modelos (tenant_id, nome) WHERE deleted_at IS NULL;
