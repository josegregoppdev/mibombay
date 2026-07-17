package com.mibombay.sistemaresurante.models;

import com.mibombay.sistemaresurante.models.enums.MovimientoTipo;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_inventario")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long empresaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "item_tipo", nullable = false, length = 20)
    private String itemTipo;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_nombre", length = 150)
    private String itemNombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "movimiento_tipo", nullable = false, length = 30)
    private MovimientoTipo movimientoTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(nullable = false, length = 1)
    private String signo;

    @Column(name = "stock_anterior", precision = 12, scale = 3)
    private BigDecimal stockAnterior;

    @Column(name = "stock_posterior", precision = 12, scale = 3)
    private BigDecimal stockPosterior;

    @Column(name = "fecha_movimiento", nullable = false, updatable = false)
    private LocalDateTime fechaMovimiento;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @PrePersist
    protected void onCreate() {
        if (fechaMovimiento == null) {
            fechaMovimiento = LocalDateTime.now();
        }
    }
}
