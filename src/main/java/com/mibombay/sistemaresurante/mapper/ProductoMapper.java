package com.mibombay.sistemaresurante.mapper;

import com.mibombay.sistemaresurante.DTO.ProductoDTO;
import com.mibombay.sistemaresurante.models.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    ProductoDTO toDTO(Producto producto);

    Producto toEntity(ProductoDTO dto);

    void updateEntity(@MappingTarget Producto producto, ProductoDTO dto);
}
