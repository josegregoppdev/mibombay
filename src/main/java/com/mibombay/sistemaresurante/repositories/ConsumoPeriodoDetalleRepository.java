package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.ConsumoPeriodoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsumoPeriodoDetalleRepository extends JpaRepository<ConsumoPeriodoDetalle, Long> {

    List<ConsumoPeriodoDetalle> findByConsumoPeriodoIdAndActivoTrue(Long consumoPeriodoId);

    List<ConsumoPeriodoDetalle> findByConsumoPeriodoIdAndActivoTrueOrderByItemNombreAsc(Long consumoPeriodoId);
}
