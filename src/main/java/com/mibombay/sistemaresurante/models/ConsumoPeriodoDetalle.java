package com.mibombay.sistemaresurante.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "consumo_periodo_detalle")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsumoPeriodoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumo_periodo_id", nullable = false)
    private Long consumoPeriodoId;

    @Column(name = "item_tipo", nullable = false, length = 20)
    private String itemTipo;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_nombre", length = 150)
    private String itemNombre;

    @Column(name = "stock_sistema", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal stockSistema = BigDecimal.ZERO;

    @Column(name = "stock_final", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal stockFinal = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal merma = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal desperdicio = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @PrePersist
    protected void onCreate() {
        if (activo == null) activo = true;
        if (stockSistema == null) stockSistema = BigDecimal.ZERO;
        if (stockFinal == null) stockFinal = BigDecimal.ZERO;
        if (merma == null) merma = BigDecimal.ZERO;
        if (desperdicio == null) desperdicio = BigDecimal.ZERO;
    }
}
