package com.mibombay.sistemaresurante.DTO;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ConsumoPeriodoFormDTO {

    private List<ItemConsumo> items;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ItemConsumo {
        private String itemTipo;
        private Long itemId;
        private BigDecimal stockFinal;
        private BigDecimal merma;
        private BigDecimal desperdicio;
    }
}
