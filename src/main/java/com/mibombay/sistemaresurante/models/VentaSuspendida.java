package com.mibombay.sistemaresurante.models;

import com.mibombay.sistemaresurante.models.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "venta_suspendida")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@SQLDelete(sql = "UPDATE venta_suspendida SET activo = false WHERE id = ?")
@Where(clause = "activo = true")
public class VentaSuspendida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long empresaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private String etiqueta;

    @Column(name = "items_json", columnDefinition = "TEXT", nullable = false)
    private String itemsJson;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "cliente_nombre")
    private String clienteNombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 15)
    private MetodoPago metodoPago;

    @Column(name = "recibido_efectivo", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal recibidoEfectivo = BigDecimal.ZERO;

    @Column(name = "recibido_transferencia", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal recibidoTransferencia = BigDecimal.ZERO;

    @Column(name = "para_llevar")
    @Builder.Default
    private Boolean paraLlevar = false;

    @Column(name = "orden_tab", nullable = false)
    @Builder.Default
    private Integer ordenTab = 1;

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
        fechaActualizacion = LocalDateTime.now();
        if (activo == null) activo = true;
        if (paraLlevar == null) paraLlevar = false;
        if (recibidoEfectivo == null) recibidoEfectivo = BigDecimal.ZERO;
        if (recibidoTransferencia == null) recibidoTransferencia = BigDecimal.ZERO;
        if (ordenTab == null) ordenTab = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
