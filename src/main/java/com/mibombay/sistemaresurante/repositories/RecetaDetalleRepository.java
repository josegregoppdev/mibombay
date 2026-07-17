package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.RecetaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecetaDetalleRepository extends JpaRepository<RecetaDetalle, Long> {

    List<RecetaDetalle> findByRecetaId(Long recetaId);

    boolean existsByIngredienteIdAndRecetaId(Long ingredienteId, Long recetaId);

    boolean existsByIngredienteId(Long ingredienteId);
}
