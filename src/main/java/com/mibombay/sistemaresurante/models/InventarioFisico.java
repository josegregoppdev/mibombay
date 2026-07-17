package com.mibombay.sistemaresurante.models;

import com.mibombay.sistemaresurante.models.enums.InventarioEstado;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventario_fisico")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLDelete(sql = "UPDATE inventario_fisico SET activo = false WHERE id = ?")
@Where(clause = "activo = true")
public class InventarioFisico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long empresaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private InventarioEstado estado = InventarioEstado.BORRADOR;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @Column(name = "usuario_confirmacion_id")
    private Long usuarioConfirmacionId;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        if (fecha == null) fecha = LocalDate.now();
        if (estado == null) estado = InventarioEstado.BORRADOR;
        if (activo == null) activo = true;
    }
}
