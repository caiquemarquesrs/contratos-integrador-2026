-- Tarefa 10: documentos associados a um contrato (RF-08). Roda como own_contratos.
-- Faixa de migrations desta tarefa: V20 a V29.
--
-- O arquivo fica na própria linha do documento (coluna "conteudo"), com limite de 5 MB por arquivo
-- (a aplicação confere). O contrato diz que o tipo de armazenamento é decisão de implementação. Só a classe
-- ArmazenamentoNoBanco sabe disso: trocar por disco ou MinIO depois não muda os endpoints.

-- As sete colunas obrigatórias da §7.2: id, tenant_id, created_at, updated_at, deleted_at,
-- created_by e updated_by.
CREATE TABLE contratos.documentos (
  id            uuid PRIMARY KEY,
  tenant_id     uuid NOT NULL,
  contrato_id   uuid NOT NULL REFERENCES contratos.contratos (id),
  nome          varchar(255) NOT NULL,
  tipo_arquivo  varchar(150) NOT NULL,
  tamanho_bytes bigint NOT NULL CHECK (tamanho_bytes >= 0),
  conteudo      bytea,          -- vira NULL quando o documento é excluído (a linha fica, por auditoria)
  created_at    timestamptz NOT NULL,
  updated_at    timestamptz NOT NULL,
  deleted_at    timestamptz,
  created_by    uuid,
  updated_by    uuid
);
CREATE INDEX idx_documentos_contrato ON contratos.documentos (contrato_id, created_at DESC) WHERE deleted_at IS NULL;
