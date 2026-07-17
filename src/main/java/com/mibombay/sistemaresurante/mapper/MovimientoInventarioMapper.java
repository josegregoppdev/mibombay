package com.mibombay.sistemaresurante.mapper;

import com.mibombay.sistemaresurante.DTO.MovimientoInventarioDTO;
import com.mibombay.sistemaresurante.models.MovimientoInventario;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MovimientoInventarioMapper {

    MovimientoInventarioDTO toDTO(MovimientoInventario entity);

    MovimientoInventario toEntity(MovimientoInventarioDTO dto);
}
