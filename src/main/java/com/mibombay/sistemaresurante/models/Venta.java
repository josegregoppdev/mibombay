package com.mibombay.sistemaresurante.models;

import com.mibombay.sistemaresurante.models.enums.MetodoPago;
import com.mibombay.sistemaresurante.models.enums.TipoVenta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long empresaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_venta", nullable = false, length = 10)
    private TipoVenta tipoVenta;

    @Column(name = "fecha_venta", nullable = false, updatable = false)
    private LocalDateTime fechaVenta;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 15)
    private MetodoPago metodoPago;

    @Column(name = "recibido_efectivo", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal recibidoEfectivo = BigDecimal.ZERO;

    @Column(name = "recibido_transferencia", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal recibidoTransferencia = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cambio = BigDecimal.ZERO;

    @Column(name = "para_llevar")
    @Builder.Default
    private Boolean paraLlevar = false;

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
        if (fechaVenta == null) fechaVenta = LocalDateTime.now();
        if (activo == null) activo = true;
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        if (total == null) total = BigDecimal.ZERO;
        if (cambio == null) cambio = BigDecimal.ZERO;
    }
}
