-- Número do contrato: CT-{ano}-{sequência de 6 dígitos}. Sequence do banco = sem corrida entre requisições.
CREATE SEQUENCE contratos.seq_numero_contrato;

CREATE TABLE contratos.contratos (
  id                        uuid PRIMARY KEY,
  tenant_id                 uuid NOT NULL,
  numero                    varchar(20) NOT NULL,
  empresa_id                uuid NOT NULL,
  empresa_nome              varchar(255),                    -- cópia do nome no cadastro (§9.5); veja o passo 5
  modelo_id                 uuid,                            -- SEM foreign key: a tabela de modelos nasce na V10
  status                    varchar(30) NOT NULL,
  valor_mensal              numeric(15,2) NOT NULL,
  mrr                       numeric(15,2) NOT NULL,
  data_inicio               date NOT NULL,
  vigencia_meses            integer,
  vigencia_fim              date,                            -- derivada: data_inicio + vigencia_meses (não aparece no JSON)
  renovacao                 varchar(20) NOT NULL,
  reajuste_anual_percentual numeric(9,4),
  indice_reajuste           varchar(10) NOT NULL DEFAULT 'nenhum',
  aviso_previo_dias         integer NOT NULL,
  responsavel_id            uuid NOT NULL,
  origem                    varchar(100) NOT NULL,
  origem_modulo_id          varchar(255) NOT NULL,
  assinado_em               timestamptz,                     -- usadas na Tarefa 3
  assinado_por              uuid,
  created_at                timestamptz NOT NULL,
  updated_at                timestamptz NOT NULL,
  deleted_at                timestamptz,
  created_by                uuid,
  updated_by                uuid,
  CONSTRAINT ck_contratos_status CHECK (status IN ('RASCUNHO','EM_APROVACAO','AGUARDANDO_ASSINATURA',
      'ATIVO','A_VENCER','RENOVADO','SUSPENSO','CANCELADO','FINALIZADO')),
  CONSTRAINT ck_contratos_renovacao CHECK (renovacao IN ('automatica','manual','sem_renovacao')),
  CONSTRAINT ck_contratos_indice CHECK (indice_reajuste IN ('IPCA','IGPM','INPC','nenhum')),
  CONSTRAINT ck_contratos_vigencia CHECK (vigencia_meses IS NULL OR vigencia_meses >= 1),
  CONSTRAINT ck_contratos_aviso CHECK (aviso_previo_dias >= 0)
);
CREATE UNIQUE INDEX ux_contratos_numero ON contratos.contratos (tenant_id, numero);
CREATE INDEX idx_contratos_tenant ON contratos.contratos (tenant_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_contratos_empresa ON contratos.contratos (tenant_id, empresa_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contratos_status ON contratos.contratos (tenant_id, status) WHERE deleted_at IS NULL;

CREATE TABLE contratos.servicos_contratados (
  id               uuid PRIMARY KEY,
  tenant_id        uuid NOT NULL,
  contrato_id      uuid NOT NULL REFERENCES contratos.contratos (id),
  item_catalogo_id uuid,
  descricao        varchar(255) NOT NULL,
  quantidade       numeric(15,4) NOT NULL CHECK (quantidade > 0),   -- até 4 casas: o catálogo vende GB, TB e hora fracionados
  valor_unitario   numeric(15,2) NOT NULL,
  valor_total      numeric(15,2) NOT NULL,
  created_at       timestamptz NOT NULL,
  updated_at       timestamptz NOT NULL,
  deleted_at       timestamptz,
  created_by       uuid,
  updated_by       uuid
);
CREATE INDEX idx_servicos_contrato ON contratos.servicos_contratados (contrato_id) WHERE deleted_at IS NULL;
