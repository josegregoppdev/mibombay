package com.mibombay.sistemaresurante.mapper;

import com.mibombay.sistemaresurante.DTO.ProveedorDTO;
import com.mibombay.sistemaresurante.models.Proveedor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProveedorMapper {

    ProveedorDTO toDTO(Proveedor proveedor);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Proveedor toEntity(ProveedorDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "empresaId", ignore = true)
    @Mapping(target = "usuarioId", ignore = true)
    @Mapping(target = "esProveedorDefecto", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    void updateEntity(@MappingTarget Proveedor proveedor, ProveedorDTO dto);
}
