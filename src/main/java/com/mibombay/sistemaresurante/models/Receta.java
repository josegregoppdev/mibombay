package com.mibombay.sistemaresurante.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recetas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long empresaId;

    @Column(name = "producto_id", nullable = false, unique = true)
    private Long productoId;

    @Column(name = "nombre_receta", length = 120)
    private String nombreReceta;

    @Column(name = "costo_receta", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costoReceta = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @OneToMany(mappedBy = "recetaId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RecetaDetalle> detalles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (activo == null) activo = true;
        if (costoReceta == null) costoReceta = BigDecimal.ZERO;
    }
}
