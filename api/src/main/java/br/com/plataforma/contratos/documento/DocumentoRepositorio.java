package br.com.plataforma.contratos.documento;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DocumentoRepositorio extends JpaRepository<Documento, UUID> {

    /** Consulta, não findById: só a consulta respeita o filtro de tenant (veja o ModeloRepositorio). */
    @Query("select d from Documento d where d.id = :id")
    Optional<Documento> buscarPorId(UUID id);

    @Query("select d from Documento d where d.contratoId = :contratoId")
    Page<Documento> doContrato(UUID contratoId, Pageable pagina);
}
