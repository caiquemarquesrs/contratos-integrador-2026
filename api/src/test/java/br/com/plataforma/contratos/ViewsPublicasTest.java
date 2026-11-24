package br.com.plataforma.contratos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/** V21: views públicas de relatório (contratos.views.md, Contrato §9.8). */
class ViewsPublicasTest extends BaseContratos {

    @Test
    void viewsMostramOContratoESeusServicosESomemComAExclusaoLogica() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarContrato(tenant);

        assertThat(jdbc.queryForObject(
                "select status from contratos.vw_pub_contratos where id = ?", String.class, id))
                .isEqualTo("EM_APROVACAO");
        assertThat(jdbc.queryForObject(
                "select count(*) from contratos.vw_pub_servicos_contratados where contrato_id = ?", Long.class, id))
                .isEqualTo(1L);

        jdbc.update("update contratos.contratos set deleted_at = now() where id = ?", id);

        assertThat(jdbc.queryForObject(
                "select count(*) from contratos.vw_pub_contratos where id = ?", Long.class, id)).isZero();
        assertThat(jdbc.queryForObject(
                "select count(*) from contratos.vw_pub_servicos_contratados where contrato_id = ?", Long.class, id))
                .isZero();
    }
}
