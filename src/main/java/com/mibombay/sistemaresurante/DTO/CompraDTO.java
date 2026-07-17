package com.mibombay.sistemaresurante.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompraDTO {

    private Long id;
    private Long empresaId;
    private Long usuarioId;

    @NotNull(message = "El proveedor es obligatorio")
    private Long proveedorId;
    private String nombreProveedor;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDateTime fechaCompra;

    private String numeroFactura;
    private String observaciones;

    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    private String tipo;

    @Builder.Default
    private Boolean activo = true;
    private LocalDateTime fechaCreacion;

    @NotNull(message = "Debe agregar al menos un detalle")
    private List<CompraDetalleDTO> detalles;
}
