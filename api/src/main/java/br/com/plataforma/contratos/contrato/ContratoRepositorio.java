package br.com.plataforma.contratos.contrato;

import java.util.Optional;
import java.util.UUID;

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
}
