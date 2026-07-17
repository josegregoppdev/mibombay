package com.mibombay.sistemaresurante.DTO;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InventarioFisicoFormDTO {

    private Long inventarioId;
    private List<ItemStock> items = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ItemStock {
        private Long itemId;
        private String itemTipo;
        private BigDecimal stockFisico;
        private BigDecimal merma;
        private BigDecimal desperdicio;
    }
}
