package com.mibombay.sistemaresurante.DTO;

import com.mibombay.sistemaresurante.models.enums.MetodoPago;
import com.mibombay.sistemaresurante.models.enums.TipoVenta;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VentaDTO {

    private Long id;
    private Long empresaId;
    private Long usuarioId;
    private String nombreUsuario;

    @NotNull(message = "El tipo de venta es obligatorio")
    @Builder.Default
    private TipoVenta tipoVenta = TipoVenta.BARRA;

    private LocalDateTime fechaVenta;

    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    @Builder.Default
    private BigDecimal recibidoEfectivo = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal recibidoTransferencia = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal cambio = BigDecimal.ZERO;

    @Builder.Default
    private Boolean paraLlevar = false;

    private String observaciones;

    @Builder.Default
    private Boolean activo = true;
    private LocalDateTime fechaCreacion;

    @NotNull(message = "Debe agregar al menos un detalle")
    private List<VentaDetalleDTO> detalles;

    private Long clienteId;
    private String nombreCliente;
}
