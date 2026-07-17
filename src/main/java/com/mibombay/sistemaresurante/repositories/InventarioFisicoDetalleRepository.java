package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.InventarioFisicoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioFisicoDetalleRepository extends JpaRepository<InventarioFisicoDetalle, Long> {

    List<InventarioFisicoDetalle> findByInventarioFisicoIdAndActivoTrueOrderByItemTipoAscItemNombreAsc(Long inventarioFisicoId);

    List<InventarioFisicoDetalle> findByInventarioFisicoIdAndActivoTrue(Long inventarioFisicoId);

    @Query("SELECT d FROM InventarioFisicoDetalle d " +
           "JOIN InventarioFisico i ON i.id = d.inventarioFisicoId " +
           "WHERE d.itemTipo = :itemTipo AND d.itemId = :itemId " +
           "AND i.empresaId = :empresaId AND i.estado = 'CONFIRMADO' " +
           "AND i.activo = true AND d.activo = true " +
           "ORDER BY i.fecha DESC, i.id DESC")
    List<InventarioFisicoDetalle> findUltimoConfirmadoByItem(@Param("itemTipo") String itemTipo,
                                                              @Param("itemId") Long itemId,
                                                              @Param("empresaId") Long empresaId);

    @Query("SELECT d FROM InventarioFisicoDetalle d " +
           "JOIN InventarioFisico i ON i.id = d.inventarioFisicoId " +
           "WHERE d.itemTipo = :itemTipo AND d.itemId = :itemId " +
           "AND i.empresaId = :empresaId AND i.estado = 'CONFIRMADO' " +
           "AND i.activo = true AND d.activo = true " +
           "AND i.fecha <= :fecha " +
           "ORDER BY i.fecha DESC, i.id DESC")
    List<InventarioFisicoDetalle> findUltimoConfirmadoByItemAntesDe(@Param("itemTipo") String itemTipo,
                                                                     @Param("itemId") Long itemId,
                                                                     @Param("empresaId") Long empresaId,
                                                                     @Param("fecha") LocalDate fecha);

    @Query("SELECT COALESCE(SUM(d.merma), 0) FROM InventarioFisicoDetalle d " +
           "JOIN InventarioFisico i ON i.id = d.inventarioFisicoId " +
           "WHERE d.itemTipo = :itemTipo AND d.itemId = :itemId " +
           "AND i.empresaId = :empresaId AND i.estado = 'CONFIRMADO' " +
           "AND i.activo = true AND d.activo = true " +
           "AND i.fecha BETWEEN :desde AND :hasta")
    BigDecimal sumMermaByItemBetween(@Param("itemTipo") String itemTipo,
                                      @Param("itemId") Long itemId,
                                      @Param("empresaId") Long empresaId,
                                      @Param("desde") LocalDate desde,
                                      @Param("hasta") LocalDate hasta);

    @Query("SELECT COALESCE(SUM(d.desperdicio), 0) FROM InventarioFisicoDetalle d " +
           "JOIN InventarioFisico i ON i.id = d.inventarioFisicoId " +
           "WHERE d.itemTipo = :itemTipo AND d.itemId = :itemId " +
           "AND i.empresaId = :empresaId AND i.estado = 'CONFIRMADO' " +
           "AND i.activo = true AND d.activo = true " +
           "AND i.fecha BETWEEN :desde AND :hasta")
    BigDecimal sumDesperdicioByItemBetween(@Param("itemTipo") String itemTipo,
                                             @Param("itemId") Long itemId,
                                             @Param("empresaId") Long empresaId,
                                             @Param("desde") LocalDate desde,
                                             @Param("hasta") LocalDate hasta);

    @Query("SELECT COALESCE(SUM(d.diferencia), 0) FROM InventarioFisicoDetalle d " +
           "JOIN InventarioFisico i ON i.id = d.inventarioFisicoId " +
           "WHERE d.itemTipo = :itemTipo AND d.itemId = :itemId " +
           "AND i.empresaId = :empresaId AND i.estado = 'CONFIRMADO' " +
           "AND i.activo = true AND d.activo = true " +
           "AND i.fecha BETWEEN :desde AND :hasta")
    BigDecimal sumDiferenciaByItemBetween(@Param("itemTipo") String itemTipo,
                                           @Param("itemId") Long itemId,
                                           @Param("empresaId") Long empresaId,
                                           @Param("desde") LocalDate desde,
                                           @Param("hasta") LocalDate hasta);
}
