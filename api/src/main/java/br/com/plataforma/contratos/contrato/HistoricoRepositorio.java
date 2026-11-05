package br.com.plataforma.contratos.contrato;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HistoricoRepositorio extends JpaRepository<HistoricoContrato, UUID> {

    /** Como consulta, não como findBy: o filtro de tenant do Hibernate vale para consultas. */
    @Query("select h from HistoricoContrato h where h.contratoId = :contratoId")
    Page<HistoricoContrato> listar(UUID contratoId, Pageable pageable);
}
