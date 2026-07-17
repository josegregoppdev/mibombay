package com.mibombay.sistemaresurante.mapper;

import com.mibombay.sistemaresurante.DTO.request.EmpresaRequest;
import com.mibombay.sistemaresurante.DTO.response.EmpresaResponse;
import com.mibombay.sistemaresurante.models.Empresa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EmpresaMapper {

    EmpresaResponse toResponse(Empresa empresa);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Empresa toEntity(EmpresaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    void updateEntity(@MappingTarget Empresa empresa, EmpresaRequest request);
}
