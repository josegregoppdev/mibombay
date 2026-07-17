package com.mibombay.sistemaresurante.mapper;

import com.mibombay.sistemaresurante.DTO.IngredienteDTO;
import com.mibombay.sistemaresurante.models.Ingrediente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface IngredienteMapper {

    IngredienteDTO toDTO(Ingrediente ingrediente);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Ingrediente toEntity(IngredienteDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "empresaId", ignore = true)
    @Mapping(target = "usuarioId", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "consumible", ignore = true)
    void updateEntity(@MappingTarget Ingrediente ingrediente, IngredienteDTO dto);
}
