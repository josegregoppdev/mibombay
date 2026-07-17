package com.mibombay.sistemaresurante.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cierre_z")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CierreZ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long empresaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "total_ventas", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(name = "cantidad_ventas")
    @Builder.Default
    private Integer cantidadVentas = 0;

    @Column(name = "total_efectivo", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEfectivo = BigDecimal.ZERO;

    @Column(name = "total_transferencia", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalTransferencia = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        if (activo == null) activo = true;
        if (totalVentas == null) totalVentas = BigDecimal.ZERO;
        if (totalEfectivo == null) totalEfectivo = BigDecimal.ZERO;
        if (totalTransferencia == null) totalTransferencia = BigDecimal.ZERO;
        if (cantidadVentas == null) cantidadVentas = 0;
    }
}
