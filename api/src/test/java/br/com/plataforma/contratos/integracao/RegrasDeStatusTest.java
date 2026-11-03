package br.com.plataforma.contratos.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.plataforma.contratos.contrato.Contrato;
import br.com.plataforma.contratos.contrato.StatusContrato;
import br.com.plataforma.contratos.integracao.StatusContratual.Situacao;

/**
 * Regras do status contratual, do formato de dinheiro e do padrão de busca, sem banco e com
 * data fixa: as bordas (último dia da vigência, janela de aviso) não dependem do dia em que o teste roda.
 */
class RegrasDeStatusTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 3);
    private static final UUID EMPRESA = UUID.randomUUID();

    // ---------------------------------------------------------------- situação de um contrato

    @Test
    void suspensoEHaSemprePrecisaDeAcao() {
        assertEquals(Situacao.CRITICO, situacao(StatusContrato.SUSPENSO, null, "automatica", 30));
        assertEquals(Situacao.CRITICO, situacao(StatusContrato.SUSPENSO, HOJE.plusDays(200), "automatica", 30));
    }

    @Test
    void vencidoSemRenovacaoAutomaticaECritico() {
        assertEquals(Situacao.CRITICO, situacao(StatusContrato.ATIVO, HOJE.minusDays(1), "manual", 30));
        assertEquals(Situacao.CRITICO, situacao(StatusContrato.ATIVO, HOJE.minusDays(1), "sem_renovacao", 30));
    }

    @Test
    void vencidoComRenovacaoAutomaticaEAtencao() {
        assertEquals(Situacao.ATENCAO, situacao(StatusContrato.ATIVO, HOJE.minusDays(1), "automatica", 30));
    }

    @Test
    void noUltimoDiaDaVigenciaAindaNaoEstaVencido() {
        // vigenciaFim == hoje: dentro da janela de aviso (atenção), não vencido (crítico)
        assertEquals(Situacao.ATENCAO, situacao(StatusContrato.ATIVO, HOJE, "manual", 30));
    }

    @Test
    void aVencerEAtencaoMesmoSemVigenciaFim() {
        assertEquals(Situacao.ATENCAO, situacao(StatusContrato.A_VENCER, null, "manual", 30));
    }

    @Test
    void janelaDeAvisoComecaExatamenteNoDiaDoAvisoPrevio() {
        assertEquals(Situacao.ATENCAO, situacao(StatusContrato.ATIVO, HOJE.plusDays(30), "manual", 30));
        assertEquals(Situacao.OK, situacao(StatusContrato.ATIVO, HOJE.plusDays(31), "manual", 30));
    }

    @Test
    void semAvisoPrevioAJanelaEOProprioUltimoDia() {
        assertEquals(Situacao.OK, situacao(StatusContrato.ATIVO, HOJE.plusDays(1), "manual", 0));
        assertEquals(Situacao.ATENCAO, situacao(StatusContrato.ATIVO, HOJE, "manual", 0));
    }

    @Test
    void semVigenciaFimNaoTemPrazoParaVencer() {
        assertEquals(Situacao.OK, situacao(StatusContrato.ATIVO, null, "manual", 30));
        assertEquals(Situacao.OK, situacao(StatusContrato.RENOVADO, null, "automatica", 30));
    }

    @Test
    void contratoLongeDoFimEOk() {
        assertEquals(Situacao.OK, situacao(StatusContrato.ATIVO, HOJE.plusDays(365), "automatica", 30));
    }

    // ---------------------------------------------------------------- escolha do contrato da empresa

    @Test
    void semContratoVigenteEhSemContratoComCamposNulos() {
        StatusContratual status = IntegracaoServico.statusDaEmpresa(EMPRESA, List.of(), HOJE);

        assertEquals(EMPRESA, status.empresaId());
        assertEquals(Situacao.SEM_CONTRATO, status.situacao());
        assertNull(status.contratoId());
        assertNull(status.numero());
        assertNull(status.diasParaVencer());
    }

    @Test
    void empresaFicaComAPiorSituacaoEOContratoQueACausou() {
        Contrato ok = contrato("CT-1", StatusContrato.ATIVO, HOJE.plusDays(300), "automatica", 30);
        Contrato atencao = contrato("CT-2", StatusContrato.A_VENCER, HOJE.plusDays(10), "automatica", 30);
        Contrato critico = contrato("CT-3", StatusContrato.SUSPENSO, HOJE.plusDays(500), "automatica", 30);

        StatusContratual status = IntegracaoServico.statusDaEmpresa(EMPRESA, List.of(ok, atencao, critico), HOJE);

        assertEquals(Situacao.CRITICO, status.situacao());
        assertEquals(critico.getId(), status.contratoId());
        assertEquals("CT-3", status.numero());
        assertEquals(500, status.diasParaVencer());
    }

    @Test
    void emEmpateVaiOContratoDeMenorVigenciaFim() {
        Contrato longe = contrato("CT-1", StatusContrato.SUSPENSO, HOJE.plusDays(90), "automatica", 30);
        Contrato perto = contrato("CT-2", StatusContrato.SUSPENSO, HOJE.plusDays(20), "automatica", 30);

        StatusContratual status = IntegracaoServico.statusDaEmpresa(EMPRESA, List.of(longe, perto), HOJE);

        assertEquals(perto.getId(), status.contratoId());
        assertEquals(20, status.diasParaVencer());
    }

    @Test
    void emEmpateSemVigenciaFimFicaPorUltimo() {
        Contrato semPrazo = contrato("CT-1", StatusContrato.SUSPENSO, null, "automatica", 30);
        Contrato comPrazo = contrato("CT-2", StatusContrato.SUSPENSO, HOJE.plusDays(400), "automatica", 30);

        StatusContratual status = IntegracaoServico.statusDaEmpresa(EMPRESA, List.of(semPrazo, comPrazo), HOJE);

        assertEquals(comPrazo.getId(), status.contratoId());
    }

    @Test
    void resultadoNaoDependeDaOrdemEmQueOBancoDevolveOsContratos() {
        Contrato a = contrato("CT-1", StatusContrato.SUSPENSO, null, "automatica", 30);
        Contrato b = contrato("CT-2", StatusContrato.SUSPENSO, null, "automatica", 30);

        assertEquals("CT-1", IntegracaoServico.statusDaEmpresa(EMPRESA, List.of(a, b), HOJE).numero());
        assertEquals("CT-1", IntegracaoServico.statusDaEmpresa(EMPRESA, List.of(b, a), HOJE).numero());
    }

    // ---------------------------------------------------------------- dias para vencer

    @Test
    void diasParaVencer() {
        assertNull(IntegracaoServico.diasParaVencer(null, HOJE));
        assertEquals(10, IntegracaoServico.diasParaVencer(HOJE.plusDays(10), HOJE));
        assertEquals(0, IntegracaoServico.diasParaVencer(HOJE, HOJE));
        assertEquals(0, IntegracaoServico.diasParaVencer(HOJE.minusDays(15), HOJE));
    }

    // ---------------------------------------------------------------- dinheiro e busca

    @Test
    void dinheiroSaiNoFormatoBrasileiroComEspacoComum() {
        assertEquals("R$ 1.890,00", IntegracaoServico.dinheiro(new BigDecimal("1890.00")));
        assertEquals("R$ 0,50", IntegracaoServico.dinheiro(new BigDecimal("0.50")));
        assertEquals("R$ 1.234.567,89", IntegracaoServico.dinheiro(new BigDecimal("1234567.89")));
        assertFalse(IntegracaoServico.dinheiro(new BigDecimal("1890.00")).contains("\u00a0"));
    }

    @Test
    void padraoDeBuscaEscapaOsCuringasDoLike() {
        assertEquals("%abc%", IntegracaoServico.padraoDeBusca("ABC"));
        assertEquals("%50!%%", IntegracaoServico.padraoDeBusca("50%"));
        assertEquals("%a!_b%", IntegracaoServico.padraoDeBusca("a_b"));
        assertEquals("%a!!b%", IntegracaoServico.padraoDeBusca("a!b"));
        assertEquals("%c:\\dir%", IntegracaoServico.padraoDeBusca("C:\\dir"));
    }

    // ---------------------------------------------------------------- apoio

    private static Situacao situacao(StatusContrato status, LocalDate fim, String renovacao, int aviso) {
        return IntegracaoServico.situacaoDoContrato(status, fim, renovacao, aviso, HOJE);
    }

    /** A entidade não tem setter de status nem de vigenciaFim (mudam por regras de outras tarefas): aqui vão por reflexão. */
    private static Contrato contrato(String numero, StatusContrato status, LocalDate fim, String renovacao, int aviso) {
        Contrato.Dados dados = new Contrato.Dados(null, new BigDecimal("100.00"), new BigDecimal("100.00"),
                HOJE.minusMonths(6), 12, renovacao, null, "nenhum", aviso, UUID.randomUUID(), List.of());
        Contrato contrato = Contrato.novo(numero, EMPRESA, "Acme", "crm", "origem-1", dados, null);
        ReflectionTestUtils.setField(contrato, "status", status);
        ReflectionTestUtils.setField(contrato, "vigenciaFim", fim);
        return contrato;
    }
}
