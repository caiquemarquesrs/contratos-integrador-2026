package br.com.plataforma.contratos;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import br.com.plataforma.contratos.captcha.RecaptchaException;

class RecaptchaCriacaoContratoTest extends BaseContratos {

    @Test
    void encaminhaTokenDoCabecalhoAoVerificadorAntesDeCriar() throws Exception {
        UUID tenant = UUID.randomUUID();

        mvc.perform(post(BASE)
                        .with(usuario(tenant, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoNovo(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated());

        verify(recaptcha).validar("token-de-teste");
    }

    @Test
    void impedePersistenciaQuandoTokenAusente() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID empresa = UUID.randomUUID();
        long contratosAntes = jdbc.queryForObject("select count(*) from contratos.contratos", Long.class);
        doThrow(RecaptchaException.invalido()).when(recaptcha).validar(isNull());

        mvc.perform(post(BASE)
                        .with(jwt()
                                .jwt(token -> token
                                        .subject(UUID.randomUUID().toString())
                                        .claim("tenant_id", tenant.toString())
                                        .claim("perms", List.of(CRIAR)))
                                .authorities(new SimpleGrantedAuthority(CRIAR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoNovo(empresa, UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].codigo").value("RECAPTCHA_INVALIDO"));

        assertEquals(contratosAntes,
                jdbc.queryForObject("select count(*) from contratos.contratos", Long.class));
        verify(recaptcha).validar(isNull());
        verifyNoInteractions(crm);
    }
}
