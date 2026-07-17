package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.ConsumoPeriodo;
import com.mibombay.sistemaresurante.models.enums.ConsumoEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsumoPeriodoRepository extends JpaRepository<ConsumoPeriodo, Long> {

    List<ConsumoPeriodo> findByEmpresaIdAndActivoTrueOrderByFechaFinDescIdDesc(Long empresaId);

    boolean existsByEmpresaIdAndTipoAndEstadoAndActivoTrue(Long empresaId, String tipo, ConsumoEstado estado);
}
