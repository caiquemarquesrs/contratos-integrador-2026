package br.com.plataforma.contratos.contrato;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ContratoRepositorio extends JpaRepository<Contrato, UUID>, JpaSpecificationExecutor<Contrato> {

    /**
     * Busca por id como consulta, não como findById: o filtro de tenant do Hibernate vale para
     * consultas, e assim um id de outra empresa responde "não encontrado".
     */
    @Query("select c from Contrato c where c.id = :id")
    Optional<Contrato> buscarPorId(UUID id);

    /**
     * Vários ids de uma vez (resumo em lote). Também é consulta, nunca findAllById: só a consulta
     * respeita o filtro de tenant. Ids inexistentes ou de outra empresa simplesmente não voltam.
     */
    @Query("select c from Contrato c where c.id in :ids")
    List<Contrato> buscarPorIds(Collection<UUID> ids);

    /** Contratos nos estados informados de várias empresas, numa única consulta (status contratual em lote). */
    @Query("select c from Contrato c where c.empresaId in :empresaIds and c.status in :status")
    List<Contrato> vigentesDasEmpresas(Collection<UUID> empresaIds, Collection<StatusContrato> status);

    /**
     * Busca por texto no número e no nome da empresa. {@code padrao} já chega em minúsculas, com
     * os curingas do LIKE escapados por "!" (veja IntegracaoServico.padraoDeBusca); os parênteses
     * mantêm o OR isolado do filtro de tenant que o Hibernate acrescenta.
     */
    @Query("""
            select c from Contrato c
            where (lower(c.numero) like :padrao escape '!'
                   or lower(c.empresaNome) like :padrao escape '!')
            order by c.criadoEm desc, c.id""")
    List<Contrato> buscarPorTexto(String padrao, Pageable limite);
}
