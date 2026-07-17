package com.mibombay.sistemaresurante.mapper;

import com.mibombay.sistemaresurante.DTO.RecetaDTO;
import com.mibombay.sistemaresurante.models.Receta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecetaMapper {

    @Mapping(target = "nombreProducto", ignore = true)
    @Mapping(target = "detalles", ignore = true)
    @Mapping(target = "tieneReceta", ignore = true)
    RecetaDTO toDTO(Receta receta);

    Receta toEntity(RecetaDTO dto);
}
