package com.mibombay.sistemaresurante.mapper;

import com.mibombay.sistemaresurante.DTO.RecetaDetalleDTO;
import com.mibombay.sistemaresurante.models.RecetaDetalle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecetaDetalleMapper {

    @Mapping(target = "nombreIngrediente", ignore = true)
    @Mapping(target = "unidad", ignore = true)
    RecetaDetalleDTO toDTO(RecetaDetalle detalle);

    RecetaDetalle toEntity(RecetaDetalleDTO dto);
}
