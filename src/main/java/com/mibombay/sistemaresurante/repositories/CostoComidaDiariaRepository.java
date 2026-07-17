package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.CostoComidaDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CostoComidaDiariaRepository extends JpaRepository<CostoComidaDiaria, Long> {

    Optional<CostoComidaDiaria> findByEmpresaIdAndFechaAndActivoTrue(Long empresaId, LocalDate fecha);

    List<CostoComidaDiaria> findByEmpresaIdAndFechaBetweenAndActivoTrueOrderByFechaAsc(Long empresaId, LocalDate desde, LocalDate hasta);

    boolean existsByEmpresaIdAndFechaAndActivoTrue(Long empresaId, LocalDate fecha);
}
