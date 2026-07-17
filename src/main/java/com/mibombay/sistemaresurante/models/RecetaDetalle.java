package com.mibombay.sistemaresurante.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "receta_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receta_id", nullable = false)
    private Long recetaId;

    @Column(name = "ingrediente_id", nullable = false)
    private Long ingredienteId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costo = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingrediente_id", insertable = false, updatable = false)
    private Ingrediente ingrediente;

    @PrePersist
    protected void onCreate() {
        if (costo == null) costo = BigDecimal.ZERO;
    }
}
