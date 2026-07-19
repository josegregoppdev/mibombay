package com.mibombay.sistemaresurante.testdata;

import com.mibombay.sistemaresurante.DTO.IngredienteDTO;
import com.mibombay.sistemaresurante.models.Ingrediente;
import com.mibombay.sistemaresurante.models.enums.UnidadMedida;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Clase con datos de prueba (Object Mother).
 * Todos los metodos son static: se llaman directo, sin new.
 *
 * Ejemplo:
 *   Ingrediente i = IngredienteTestData.crearIngrediente(1L, "Cebolla");
 */
public class IngredienteTestData {

    // ----- Ingrediente (entidad) -----

    public static Ingrediente crearIngrediente(Long id, String nombre) {
        return Ingrediente.builder()
                .id(id)
                .empresaId(1L)
                .usuarioId(1L)
                .nombre(nombre)
                .descripcion("Descripcion de " + nombre)
                .unidadMedida(UnidadMedida.KILOGRAMO)
                .stockActual(new BigDecimal("10"))
                .stockMinimo(new BigDecimal("2"))
                .precioCompra(new BigDecimal("5000"))
                .activo(true)
                .consumible(false)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    public static Ingrediente crearIngredienteConEmpresa(Long id, String nombre, Long empresaId) {
        Ingrediente i = crearIngrediente(id, nombre);
        i.setEmpresaId(empresaId);
        return i;
    }

    public static Ingrediente crearIngredienteConsumible(Long id, String nombre) {
        Ingrediente i = crearIngrediente(id, nombre);
        i.setConsumible(true);
        return i;
    }

    public static Ingrediente crearIngredienteInactivo(Long id, String nombre) {
        Ingrediente i = crearIngrediente(id, nombre);
        i.setActivo(false);
        return i;
    }

    // ----- IngredienteDTO -----

    public static IngredienteDTO crearDTOValido() {
        return IngredienteDTO.builder()
                .nombre("Nuevo Ingrediente")
                .descripcion("Descripcion")
                .unidadMedida(UnidadMedida.KILOGRAMO)
                .stockActual(new BigDecimal("20"))
                .stockMinimo(new BigDecimal("5"))
                .precioCompra(new BigDecimal("8000"))
                .empresaId(1L)
                .activo(true)
                .consumible(false)
                .build();
    }

    public static IngredienteDTO crearDTOConEmpresaId(Long empresaId) {
        IngredienteDTO dto = crearDTOValido();
        dto.setEmpresaId(empresaId);
        return dto;
    }

    public static IngredienteDTO crearDTOResponse(Long id) {
        return IngredienteDTO.builder()
                .id(id)
                .nombre("Test")
                .unidadMedida(UnidadMedida.KILOGRAMO)
                .activo(true)
                .build();
    }
}
