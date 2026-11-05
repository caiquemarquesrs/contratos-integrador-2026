CREATE TABLE contratos.historico_contrato (
  id             uuid PRIMARY KEY,
  tenant_id      uuid NOT NULL,
  contrato_id    uuid NOT NULL REFERENCES contratos.contratos (id),
  campo          varchar(100) NOT NULL,
  valor_anterior varchar(500),
  valor_novo     varchar(500),
  usuario_id     uuid NOT NULL,
  ocorrido_em    timestamptz NOT NULL,
  created_at     timestamptz NOT NULL,
  updated_at     timestamptz NOT NULL,
  deleted_at     timestamptz,
  created_by     uuid,
  updated_by     uuid
);
CREATE INDEX idx_historico_contrato ON contratos.historico_contrato (contrato_id, ocorrido_em DESC) WHERE deleted_at IS NULL;
