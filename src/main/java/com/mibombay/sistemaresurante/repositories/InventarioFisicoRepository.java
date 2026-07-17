package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.InventarioFisico;
import com.mibombay.sistemaresurante.models.enums.InventarioEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioFisicoRepository extends JpaRepository<InventarioFisico, Long> {

    List<InventarioFisico> findByEmpresaIdAndActivoTrueOrderByFechaDescIdDesc(Long empresaId);

    Optional<InventarioFisico> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);

    Optional<InventarioFisico> findByIdAndActivoTrue(Long id);

    Optional<InventarioFisico> findByEmpresaIdAndFechaAndEstadoAndActivoTrue(Long empresaId, LocalDate fecha, InventarioEstado estado);

    boolean existsByEmpresaIdAndFechaAndEstadoAndActivoTrue(Long empresaId, LocalDate fecha, InventarioEstado estado);

    long countByEmpresaIdAndActivoTrue(Long empresaId);
}
