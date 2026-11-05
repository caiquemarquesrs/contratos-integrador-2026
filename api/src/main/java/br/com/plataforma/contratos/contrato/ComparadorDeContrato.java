package br.com.plataforma.contratos.contrato;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Descobre o que mudou entre dois estados do contrato, para o histórico. Só devolve o que mudou de fato:
 * editar mandando os mesmos valores não gera nada.
 */
final class ComparadorDeContrato {

    /** Uma mudança: nome do campo e o texto do valor antes e depois (nulo = sem valor). */
    record Alteracao(String campo, String anterior, String novo) {
    }

    private ComparadorDeContrato() {
    }

    static List<Alteracao> comparar(Contrato.Dados antes, Contrato.Dados depois) {
        List<Alteracao> mudancas = new ArrayList<>();
        decimal(mudancas, "valorMensal", antes.valorMensal(), depois.valorMensal());
        decimal(mudancas, "mrr", antes.mrr(), depois.mrr());
        simples(mudancas, "dataInicio", antes.dataInicio(), depois.dataInicio());
        simples(mudancas, "vigenciaMeses", antes.vigenciaMeses(), depois.vigenciaMeses());
        simples(mudancas, "renovacao", antes.renovacao(), depois.renovacao());
        decimal(mudancas, "reajusteAnualPercentual", antes.reajusteAnualPercentual(), depois.reajusteAnualPercentual());
        simples(mudancas, "indiceReajuste", antes.indiceReajuste(), depois.indiceReajuste());
        simples(mudancas, "avisoPrevioDias", antes.avisoPrevioDias(), depois.avisoPrevioDias());
        simples(mudancas, "responsavelId", antes.responsavelId(), depois.responsavelId());
        simples(mudancas, "modeloId", antes.modeloId(), depois.modeloId());
        servicos(mudancas, antes.servicos(), depois.servicos());
        return mudancas;
    }

    private static void simples(List<Alteracao> mudancas, String campo, Object antes, Object depois) {
        if (!Objects.equals(antes, depois)) {
            mudancas.add(new Alteracao(campo, texto(antes), texto(depois)));
        }
    }

    /**
     * Compara com compareTo, nunca com equals: 1890.0 e 1890.00 são diferentes para o BigDecimal.equals e
     * viraria uma mudança que não existiu. O texto sai sempre com 2 casas, sem notação científica.
     */
    private static void decimal(List<Alteracao> mudancas, String campo, BigDecimal antes, BigDecimal depois) {
        boolean iguais = antes == null ? depois == null : depois != null && antes.compareTo(depois) == 0;
        if (!iguais) {
            mudancas.add(new Alteracao(campo, textoDecimal(antes), textoDecimal(depois)));
        }
    }

    /** Uma única linha para a lista toda, com um resumo curto: "2 serviço(s), total 3780.00". */
    private static void servicos(List<Alteracao> mudancas, List<Contrato.DadosServico> antes,
                                 List<Contrato.DadosServico> depois) {
        // a ordem em que o banco devolve os serviços não é garantida: comparar sem depender dela
        if (!chaves(antes).equals(chaves(depois))) {
            mudancas.add(new Alteracao("servicosContratados", resumo(antes), resumo(depois)));
        }
    }

    private static List<String> chaves(List<Contrato.DadosServico> servicos) {
        return servicos.stream()
                .map(s -> Objects.toString(s.itemCatalogoId(), "") + "|" + s.descricao().strip()
                        + "|" + normalizado(s.quantidade()) + "|" + normalizado(s.valorUnitario())
                        + "|" + normalizado(s.valorTotal()))
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static String resumo(List<Contrato.DadosServico> servicos) {
        BigDecimal total = servicos.stream()
                .map(Contrato.DadosServico::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return servicos.size() + " serviço(s), total " + textoDecimal(total);
    }

    private static String normalizado(BigDecimal valor) {
        return valor == null ? "" : valor.stripTrailingZeros().toPlainString();
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static String textoDecimal(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
