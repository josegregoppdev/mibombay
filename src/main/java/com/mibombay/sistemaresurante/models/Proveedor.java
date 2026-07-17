package com.mibombay.sistemaresurante.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long empresaId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(length = 120)
    private String contacto;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String correo;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Column(name = "es_proveedor_defecto")
    @Builder.Default
    private Boolean esProveedorDefecto = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        if (activo == null) activo = true;
        if (esProveedorDefecto == null) esProveedorDefecto = false;
    }
}
