package com.mibombay.sistemaresurante.mapper;

import com.mibombay.sistemaresurante.DTO.VentaDTO;
import com.mibombay.sistemaresurante.DTO.VentaDetalleDTO;
import com.mibombay.sistemaresurante.models.Venta;
import com.mibombay.sistemaresurante.models.VentaDetalle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VentaMapper {

    @Mapping(target = "nombreUsuario", ignore = true)
    @Mapping(target = "nombreCliente", ignore = true)
    VentaDTO toDTO(Venta venta);

    @Mapping(target = "ingredientesExcluidosIds", ignore = true)
    VentaDetalleDTO toDetalleDTO(VentaDetalle detalle);

    VentaDetalle toDetalleEntity(VentaDetalleDTO dto);
}
