package com.mibombay.sistemaresurante.models;

import com.mibombay.sistemaresurante.models.enums.TipoItemCompra;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "compra_detalle")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompraDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "compra_id", nullable = false)
    private Long compraId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_tipo", nullable = false, length = 20)
    private TipoItemCompra itemTipo;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_nombre", length = 150)
    private String itemNombre;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @PrePersist
    protected void onCreate() {
        if (subtotal == null) subtotal = BigDecimal.ZERO;
    }
}
