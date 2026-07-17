package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    Optional<Producto> findByIdAndActivoTrue(Long id);

    boolean existsByNombreAndEmpresaIdAndActivoTrue(String nombre, Long empresaId);

    List<Producto> findAllByEmpresaIdAndTieneRecetaFalseAndActivoTrueOrderByNombreAsc(Long empresaId);

    List<Producto> findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(Long empresaId);

    @Query("SELECT COUNT(p) FROM Producto p WHERE p.empresaId = :empresaId AND p.activo = true AND p.tieneReceta = false AND p.stockActual IS NOT NULL AND p.stockActual <= :umbral")
    int countProductosStockBajo(@Param("empresaId") Long empresaId, @Param("umbral") BigDecimal umbral);
}
