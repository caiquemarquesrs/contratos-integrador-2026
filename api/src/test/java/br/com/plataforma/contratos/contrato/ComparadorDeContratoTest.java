package br.com.plataforma.contratos.contrato;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.plataforma.contratos.contrato.ComparadorDeContrato.Alteracao;
import br.com.plataforma.contratos.contrato.Contrato.Dados;
import br.com.plataforma.contratos.contrato.Contrato.DadosServico;

/** Regras do histórico sem banco: o que conta como "mudou" e como o valor vira texto. */
class ComparadorDeContratoTest {

    private static final UUID RESPONSAVEL = UUID.randomUUID();
    private static final UUID ITEM = UUID.randomUUID();

    private static DadosServico servico(String descricao, String quantidade, String unitario, String total) {
        return new DadosServico(ITEM, descricao, new BigDecimal(quantidade), new BigDecimal(unitario),
                new BigDecimal(total));
    }

    private static Dados dados(String valorMensal, DadosServico... servicos) {
        return new Dados(null, new BigDecimal(valorMensal), new BigDecimal("1500.00"), LocalDate.of(2026, 1, 15),
                12, "automatica", new BigDecimal("5.5000"), "IPCA", 30, RESPONSAVEL, List.of(servicos));
    }

    @Test
    void valoresIguaisNaoGeramMudancaMesmoComEscalasDiferentes() {
        Dados antes = dados("1890.00", servico("Hospedagem", "2.0000", "150.00", "300.00"));
        // 1890.0 e 1890.00 são diferentes para o BigDecimal.equals, mas é o mesmo valor
        Dados depois = new Dados(null, new BigDecimal("1890.0"), new BigDecimal("1500"), LocalDate.of(2026, 1, 15),
                12, "automatica", new BigDecimal("5.5"), "IPCA", 30, RESPONSAVEL,
                List.of(servico("Hospedagem", "2", "150.0", "300")));

        assertThat(ComparadorDeContrato.comparar(antes, depois)).isEmpty();
    }

    @Test
    void mudancaDeValorMensalGeraUmaLinhaComOTextoComDuasCasas() {
        Dados antes = dados("1500.00", servico("Hospedagem", "2", "150.00", "300.00"));
        Dados depois = dados("1600.5", servico("Hospedagem", "2", "150.00", "300.00"));

        assertThat(ComparadorDeContrato.comparar(antes, depois))
                .containsExactly(new Alteracao("valorMensal", "1500.00", "1600.50"));
    }

    @Test
    void campoQueVoltaAFicarNuloOuQueDeixaDeSerNuloEMudanca() {
        Dados antes = dados("1500.00", servico("Hospedagem", "2", "150.00", "300.00"));
        UUID modelo = UUID.randomUUID();
        Dados depois = new Dados(modelo, antes.valorMensal(), antes.mrr(), antes.dataInicio(), null,
                antes.renovacao(), null, antes.indiceReajuste(), antes.avisoPrevioDias(), antes.responsavelId(),
                antes.servicos());

        assertThat(ComparadorDeContrato.comparar(antes, depois)).containsExactly(
                new Alteracao("vigenciaMeses", "12", null),
                new Alteracao("reajusteAnualPercentual", "5.50", null),
                new Alteracao("modeloId", null, modelo.toString()));
    }

    @Test
    void camposSimplesSaemComOsNomesDoContrato() {
        Dados antes = dados("1500.00", servico("Hospedagem", "2", "150.00", "300.00"));
        UUID novoResponsavel = UUID.randomUUID();
        Dados depois = new Dados(null, antes.valorMensal(), new BigDecimal("1400.00"), LocalDate.of(2026, 2, 1),
                antes.vigenciaMeses(), "manual", antes.reajusteAnualPercentual(), "IGPM", 60, novoResponsavel,
                antes.servicos());

        assertThat(ComparadorDeContrato.comparar(antes, depois)).containsExactly(
                new Alteracao("mrr", "1500.00", "1400.00"),
                new Alteracao("dataInicio", "2026-01-15", "2026-02-01"),
                new Alteracao("renovacao", "automatica", "manual"),
                new Alteracao("indiceReajuste", "IPCA", "IGPM"),
                new Alteracao("avisoPrevioDias", "30", "60"),
                new Alteracao("responsavelId", RESPONSAVEL.toString(), novoResponsavel.toString()));
    }

    @Test
    void listaDeServicosMudadaGeraUmaLinhaSoComResumo() {
        Dados antes = dados("1500.00", servico("Hospedagem", "2", "150.00", "300.00"));
        Dados depois = dados("1500.00",
                servico("Hospedagem", "3", "150.00", "450.00"),
                servico("Suporte", "1", "10.00", "10.00"));

        assertThat(ComparadorDeContrato.comparar(antes, depois)).containsExactly(
                new Alteracao("servicosContratados", "1 serviço(s), total 300.00", "2 serviço(s), total 460.00"));
    }

    @Test
    void mesmaListaEmOutraOrdemNaoEMudanca() {
        DadosServico a = servico("Hospedagem", "2", "150.00", "300.00");
        DadosServico b = servico("Suporte", "1", "10.00", "10.00");

        assertThat(ComparadorDeContrato.comparar(dados("1500.00", a, b), dados("1500.00", b, a))).isEmpty();
    }

    @Test
    void valorDoHistoricoPassaDe500CaracteresECortado() {
        assertThat(HistoricoContrato.cortar("x".repeat(600))).hasSize(500);
        assertThat(HistoricoContrato.cortar("curto")).isEqualTo("curto");
        assertThat(HistoricoContrato.cortar(null)).isNull();
    }
}
