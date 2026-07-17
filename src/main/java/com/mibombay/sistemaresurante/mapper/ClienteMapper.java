package com.mibombay.sistemaresurante.mapper;

import com.mibombay.sistemaresurante.DTO.ClienteDTO;
import com.mibombay.sistemaresurante.models.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    ClienteDTO toDTO(Cliente cliente);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Cliente toEntity(ClienteDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "empresaId", ignore = true)
    @Mapping(target = "usuarioId", ignore = true)
    @Mapping(target = "esConsumidorFinal", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    void updateEntity(@MappingTarget Cliente cliente, ClienteDTO dto);
}
