package com.mibombay.sistemaresurante.mapper;

import com.mibombay.sistemaresurante.DTO.request.UsuarioRequest;
import com.mibombay.sistemaresurante.DTO.response.UsuarioResponse;
import com.mibombay.sistemaresurante.models.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    UsuarioResponse toResponse(Usuario usuario);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "esSuperadmin", ignore = true)
    Usuario toEntity(UsuarioRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "esSuperadmin", ignore = true)
    void updateEntity(@MappingTarget Usuario usuario, UsuarioRequest request);
}
