package com.mibombay.sistemaresurante.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "venta_detalle")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VentaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venta_id", nullable = false)
    private Long ventaId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "producto_nombre", length = 150)
    private String productoNombre;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String modificaciones;

    @Column(columnDefinition = "TEXT")
    private String adicionales;

    @PrePersist
    protected void onCreate() {
        if (subtotal == null) subtotal = BigDecimal.ZERO;
    }
}
