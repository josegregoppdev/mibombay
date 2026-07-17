package com.mibombay.sistemaresurante.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "costo_comida_diaria",
       uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "fecha"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLDelete(sql = "UPDATE costo_comida_diaria SET activo = false WHERE id = ?")
@Where(clause = "activo = true")
public class CostoComidaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "ventas_totales", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal ventasTotales = BigDecimal.ZERO;

    @Column(name = "costo_ingredientes_vendidos", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costoIngredientesVendidos = BigDecimal.ZERO;

    @Column(name = "food_cost_porcentaje", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal foodCostPorcentaje = BigDecimal.ZERO;

    @Column(name = "inventario_inicial_valor", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal inventarioInicialValor = BigDecimal.ZERO;

    @Column(name = "compras_valor", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal comprasValor = BigDecimal.ZERO;

    @Column(name = "inventario_final_valor", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal inventarioFinalValor = BigDecimal.ZERO;

    @Column(name = "costo_alimentos_contable", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costoAlimentosContable = BigDecimal.ZERO;

    @Column(name = "food_cost_contable_porcentaje", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal foodCostContablePorcentaje = BigDecimal.ZERO;

    @Column(name = "merma_valor", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal mermaValor = BigDecimal.ZERO;

    @Column(name = "merma_porcentaje", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal mermaPorcentaje = BigDecimal.ZERO;

    @Column(name = "desperdicio_valor", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal desperdicioValor = BigDecimal.ZERO;

    @Column(name = "desperdicio_porcentaje", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal desperdicioPorcentaje = BigDecimal.ZERO;

    @Column(name = "diferencia_valor", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal diferenciaValor = BigDecimal.ZERO;

    @Column(name = "diferencia_porcentaje", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal diferenciaPorcentaje = BigDecimal.ZERO;

    @Column(name = "consumo_indirecto_valor", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal consumoIndirectoValor = BigDecimal.ZERO;

    @Column(name = "costo_real_valor", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costoRealValor = BigDecimal.ZERO;

    @Column(name = "costo_real_porcentaje", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal costoRealPorcentaje = BigDecimal.ZERO;

    @OneToMany(mappedBy = "costoComidaDiaria", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CostoComidaDiariaItem> items = new ArrayList<>();

    public void addItem(CostoComidaDiariaItem item) {
        items.add(item);
        item.setCostoComidaDiaria(this);
    }

    public void clearItems() {
        for (CostoComidaDiariaItem item : items) {
            item.setCostoComidaDiaria(null);
        }
        items.clear();
    }

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        if (activo == null) activo = true;
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
