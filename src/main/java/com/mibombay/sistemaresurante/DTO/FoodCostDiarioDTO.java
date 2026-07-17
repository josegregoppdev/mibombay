package com.mibombay.sistemaresurante.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter @AllArgsConstructor @Builder
public class FoodCostDiarioDTO {
    private FoodCostResumenDTO resumen;
    private List<FoodCostItemDTO> items;
    private List<FoodCostCategoriaDTO> categorias;
}
