package com.mibombay.sistemaresurante.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

@Entity
@Table(name = "inventario_fisico_detalle")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLDelete(sql = "UPDATE inventario_fisico_detalle SET activo = false WHERE id = ?")
@Where(clause = "activo = true")
public class InventarioFisicoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventario_fisico_id", nullable = false)
    private Long inventarioFisicoId;

    @Column(name = "item_tipo", nullable = false, length = 20)
    private String itemTipo;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_nombre", nullable = false, length = 150)
    private String itemNombre;

    @Column(name = "unidad_medida", length = 20)
    private String unidadMedida;

    @Column(name = "stock_sistema", precision = 12, scale = 3, nullable = false)
    private BigDecimal stockSistema;

    @Column(name = "stock_fisico", precision = 12, scale = 3)
    private BigDecimal stockFisico;

    @Column(precision = 12, scale = 3)
    private BigDecimal diferencia;

    @Column(precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal merma = BigDecimal.ZERO;

    @Column(precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal desperdicio = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @PrePersist
    protected void onCreate() {
        if (activo == null) activo = true;
        if (stockSistema == null) stockSistema = BigDecimal.ZERO;
        if (merma == null) merma = BigDecimal.ZERO;
        if (desperdicio == null) desperdicio = BigDecimal.ZERO;
    }
}
