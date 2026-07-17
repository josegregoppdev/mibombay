package com.mibombay.sistemaresurante.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cuadre_caja")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLDelete(sql = "UPDATE cuadre_caja SET activo = false WHERE id = ?")
@Where(clause = "activo = true")
public class CuadreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long empresaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDate fecha;

    private String auditor;

    private String cajero;

    private String turno;

    @Column(name = "denominaciones_json", columnDefinition = "TEXT")
    private String denominacionesJson;

    @Column(name = "subtotal_efectivo", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotalEfectivo = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal transferencia = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal otros = BigDecimal.ZERO;

    @Column(name = "total_contado", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalContado = BigDecimal.ZERO;

    @Column(name = "total_sistema", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSistema = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal diferencia = BigDecimal.ZERO;

    @Column(name = "cantidad_ventas_snapshot")
    @Builder.Default
    private Integer cantidadVentasSnapshot = 0;

    @Column(name = "total_ventas_snapshot", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalVentasSnapshot = BigDecimal.ZERO;

    @Column(name = "total_efectivo_snapshot", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEfectivoSnapshot = BigDecimal.ZERO;

    @Column(name = "total_transferencia_snapshot", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalTransferenciaSnapshot = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        if (activo == null) activo = true;
    }
}
