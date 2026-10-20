-- Contratos não usa as tabelas do módulo de exemplo (Item e idempotência de consumo).
DROP VIEW  IF EXISTS contratos.vw_pub_itens;
DROP TABLE IF EXISTS contratos.eventos_processados;
DROP TABLE IF EXISTS contratos.itens;
