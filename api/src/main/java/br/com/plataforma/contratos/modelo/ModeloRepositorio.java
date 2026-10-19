package br.com.plataforma.contratos.modelo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ModeloRepositorio extends JpaRepository<Modelo, UUID> {

    /**
     * Busca por id como consulta, não como findById: o filtro de tenant do Hibernate vale para
     * consultas, e assim um id de outra empresa responde "não encontrado".
     */
    @Query("select m from Modelo m where m.id = :id")
    Optional<Modelo> buscarPorId(UUID id);
}
