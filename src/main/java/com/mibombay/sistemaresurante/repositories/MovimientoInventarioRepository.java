package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.MovimientoInventario;
import com.mibombay.sistemaresurante.models.enums.MovimientoTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long>, JpaSpecificationExecutor<MovimientoInventario> {
    List<MovimientoInventario> findByItemTipoAndItemIdOrderByFechaMovimientoAsc(String itemTipo, Long itemId);

    List<MovimientoInventario> findByItemTipoAndItemIdAndFechaMovimientoBetweenOrderByFechaMovimientoAsc(
            String itemTipo, Long itemId, LocalDateTime desde, LocalDateTime hasta);

    @Query("SELECT COALESCE(SUM(m.cantidad), 0) FROM MovimientoInventario m " +
           "WHERE m.itemTipo = :itemTipo AND m.itemId = :itemId " +
           "AND m.movimientoTipo = :tipo " +
           "AND m.fechaMovimiento BETWEEN :desde AND :hasta")
    BigDecimal sumCantidadByItemAndTipoBetween(@Param("itemTipo") String itemTipo,
                                                @Param("itemId") Long itemId,
                                                @Param("tipo") MovimientoTipo tipo,
                                                @Param("desde") LocalDateTime desde,
                                                @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(SUM(CASE WHEN m.signo = '+' THEN m.cantidad ELSE -m.cantidad END), 0) " +
           "FROM MovimientoInventario m " +
           "WHERE m.itemTipo = :itemTipo AND m.itemId = :itemId " +
           "AND m.fechaMovimiento BETWEEN :desde AND :hasta")
    BigDecimal sumNetoCambioByItemBetween(@Param("itemTipo") String itemTipo,
                                           @Param("itemId") Long itemId,
                                           @Param("desde") LocalDateTime desde,
                                           @Param("hasta") LocalDateTime hasta);

    List<MovimientoInventario> findByMovimientoTipoAndFechaMovimientoBetweenOrderByFechaMovimientoAsc(
            MovimientoTipo movimientoTipo, LocalDateTime desde, LocalDateTime hasta);

    @Query("SELECT m FROM MovimientoInventario m " +
           "WHERE m.empresaId = :empresaId AND m.movimientoTipo IN :tipos " +
           "AND m.fechaMovimiento BETWEEN :desde AND :hasta " +
           "ORDER BY m.itemTipo, m.itemId, m.fechaMovimiento")
    List<MovimientoInventario> findByEmpresaIdAndMovimientoTipoInAndFechaMovimientoBetweenOrderByItemTipoItemId(
            @Param("empresaId") Long empresaId,
            @Param("tipos") List<MovimientoTipo> tipos,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
