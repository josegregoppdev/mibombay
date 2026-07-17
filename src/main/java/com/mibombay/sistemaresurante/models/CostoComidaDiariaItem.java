package com.mibombay.sistemaresurante.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

@Entity
@Table(name = "costo_comida_diaria_item")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLDelete(sql = "UPDATE costo_comida_diaria_item SET activo = false WHERE id = ?")
@Where(clause = "activo = true")
public class CostoComidaDiariaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "costo_comida_diaria_id", nullable = false)
    private CostoComidaDiaria costoComidaDiaria;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "item_nombre")
    private String itemNombre;

    @Column(name = "item_tipo")
    private String itemTipo;

    @Column(name = "unidad_medida")
    private String unidadMedida;

    @Column(name = "cantidad_consumida", precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal cantidadConsumida = BigDecimal.ZERO;

    @Column(name = "precio_costo_unitario", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal precioCostoUnitario = BigDecimal.ZERO;

    @Column(name = "costo_grupo", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costoGrupo = BigDecimal.ZERO;

    @Column(name = "porcentaje_del_costo", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal porcentajeDelCosto = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
