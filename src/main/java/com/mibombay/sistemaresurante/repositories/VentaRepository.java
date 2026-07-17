package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    Optional<Venta> findByIdAndEmpresaId(Long id, Long empresaId);

    List<Venta> findByEmpresaIdAndFechaVentaBetweenAndActivoTrueOrderByFechaVentaDesc(Long empresaId, LocalDateTime desde, LocalDateTime hasta);

    @Query("SELECT COUNT(v) FROM Venta v WHERE v.empresaId = :empresaId AND v.fechaVenta BETWEEN :desde AND :hasta AND v.activo = true")
    int countVentasDelDia(@Param("empresaId") Long empresaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.empresaId = :empresaId AND v.fechaVenta BETWEEN :desde AND :hasta AND v.activo = true")
    BigDecimal sumTotalVentasDelDia(@Param("empresaId") Long empresaId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
