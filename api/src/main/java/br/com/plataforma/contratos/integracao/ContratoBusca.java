package br.com.plataforma.contratos.integracao;

import java.util.UUID;

/** Schema ContratoBusca do contratos.yaml: um item da busca global da plataforma. Todos os campos são obrigatórios. */
public record ContratoBusca(
        UUID id,
        String titulo,      // "CT-2026-000123 — Nome da Empresa" (só o número quando o nome não está gravado)
        String subtitulo,   // "Ativo · R$ 1.890,00/mês"
        String rota) {      // "/contratos/{id}", relativa ao módulo
}
