package br.com.plataforma.contratos.tenant;

import java.util.Map;
import java.util.UUID;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Entrega ao Hibernate o tenant da operação. Toda entidade com {@code @TenantId} passa a ser
 * filtrada por ele nas consultas e preenchida com ele na gravação — o recorte por empresa fica
 * resolvido num lugar só, em vez de um "where tenant_id = ?" em cada consulta (Requisito RF30).
 *
 * Atenção: o filtro vale para consultas JPQL e derivadas do Spring Data. SQL nativo precisa
 * filtrar tenant_id à mão.
 */
@Component
public class ResolvedorDeTenant implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    /** Sem tenant no contexto, as consultas procuram por este valor e não encontram nada. */
    static final UUID NENHUM = new UUID(0L, 0L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContexto.atual().orElse(NENHUM);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> propriedades) {
        propriedades.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
