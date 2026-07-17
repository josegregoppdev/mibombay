package com.mibombay.sistemaresurante.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @AllArgsConstructor @Builder
public class FoodCostItemDTO {
    private Long itemId;
    private String itemNombre;
    private String itemTipo;
    private String unidadMedida;
    private BigDecimal cantidadConsumida;
    private BigDecimal precioCostoUnitario;
    private BigDecimal costoGrupo;
    private BigDecimal porcentajeDelCosto;
}
