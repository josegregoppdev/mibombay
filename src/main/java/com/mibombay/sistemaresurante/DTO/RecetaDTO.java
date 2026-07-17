package com.mibombay.sistemaresurante.DTO;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetaDTO {

    private Long id;
    private Long productoId;
    private String nombreProducto;
    private String nombreReceta;
    private BigDecimal costoReceta;
    private Boolean tieneReceta;

    private List<RecetaDetalleDTO> detalles;
}
