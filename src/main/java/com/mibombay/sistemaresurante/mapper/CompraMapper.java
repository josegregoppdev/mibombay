package com.mibombay.sistemaresurante.mapper;

import com.mibombay.sistemaresurante.DTO.CompraDTO;
import com.mibombay.sistemaresurante.DTO.CompraDetalleDTO;
import com.mibombay.sistemaresurante.models.Compra;
import com.mibombay.sistemaresurante.models.CompraDetalle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompraMapper {

    @Mapping(target = "nombreProveedor", ignore = true)
    CompraDTO toDTO(Compra compra);

    @Mapping(target = "detalles", ignore = true)
    Compra toEntity(CompraDTO dto);

    CompraDetalleDTO toDetalleDTO(CompraDetalle detalle);

    CompraDetalle toDetalleEntity(CompraDetalleDTO dto);
}
