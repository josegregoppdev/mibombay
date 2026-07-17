package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.CierreZ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CierreZRepository extends JpaRepository<CierreZ, Long> {
    Optional<CierreZ> findByEmpresaIdAndFechaAndActivoTrue(Long empresaId, LocalDate fecha);
    boolean existsByEmpresaIdAndFechaAndActivoTrue(Long empresaId, LocalDate fecha);
}
