package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.CuadreCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuadreCajaRepository extends JpaRepository<CuadreCaja, Long> {

    List<CuadreCaja> findByEmpresaIdAndActivoTrueOrderByFechaCreacionDesc(Long empresaId);

    Optional<CuadreCaja> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);
}
