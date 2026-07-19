package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.IngredienteDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.mapper.IngredienteMapper;
import com.mibombay.sistemaresurante.models.Ingrediente;
import com.mibombay.sistemaresurante.repositories.IngredienteRepository;
import com.mibombay.sistemaresurante.repositories.RecetaDetalleRepository;
import com.mibombay.sistemaresurante.testdata.IngredienteTestData;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de IngredienteService con Mockito.
 *
 * Patron de cada test (3 pasos):
 *   1) preparar:  configurar mocks (when...)
 *   2) ejecutar:  llamar al metodo del service
 *   3) verificar: assertEquals / assertThrows / verify
 *
 * Los datos fake vienen de IngredienteTestData (clase aparte).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IngredienteService - Tests unitarios")
class IngredienteServiceTest {

    @Mock private IngredienteRepository ingredienteRepository;
    @Mock private IngredienteMapper ingredienteMapper;
    @Mock private RecetaDetalleRepository recetaDetalleRepository;
    @InjectMocks private IngredienteService service;

    @BeforeEach
    void setUp() {
        TenantContext.setEmpresaId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // =====================================================
    // listarIngredientesConFiltros
    // =====================================================

    @Test
    @DisplayName("listarIngredientesConFiltros: sin filtros retorna page mapeada")
    void listarIngredientesConFiltros_sinFiltros_retornaPage() {
        Ingrediente ing = IngredienteTestData.crearIngrediente(1L, "Cebolla");
        Pageable pageable = PageRequest.of(0, 15);
        when(ingredienteRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(ing)));
        when(ingredienteMapper.toDTO(ing))
                .thenReturn(IngredienteTestData.crearDTOResponse(1L));

        Page<IngredienteDTO> result = service.listarIngredientesConFiltros(1L, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(ingredienteRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("listarIngredientesConFiltros: page vacia retorna page vacia")
    void listarIngredientesConFiltros_sinResultados() {
        Pageable pageable = PageRequest.of(0, 15);
        when(ingredienteRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<IngredienteDTO> result = service.listarIngredientesConFiltros(1L, null, null, pageable);

        assertTrue(result.isEmpty());
    }

    // =====================================================
    // obtenerIngredientePorId
    // =====================================================

    @Test
    @DisplayName("obtenerIngredientePorId: mismo tenant retorna ingrediente")
    void obtenerIngredientePorId_mismoTenant() {
        Ingrediente ing = IngredienteTestData.crearIngrediente(1L, "Cebolla");
        when(ingredienteRepository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.of(ing));
        when(ingredienteMapper.toDTO(ing))
                .thenReturn(IngredienteTestData.crearDTOResponse(1L));

        IngredienteDTO result = service.obtenerIngredientePorId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("obtenerIngredientePorId: otro tenant -> 404 (IDOR)")
    void obtenerIngredientePorId_otroTenant_lanza404() {
        TenantContext.setEmpresaId(999L);
        when(ingredienteRepository.findByIdAndEmpresaIdAndActivoTrue(1L, 999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerIngredientePorId(1L));
    }

    @Test
    @DisplayName("obtenerIngredientePorId: no existe -> 404")
    void obtenerIngredientePorId_noExiste_lanza404() {
        when(ingredienteRepository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerIngredientePorId(1L));
    }

    // =====================================================
    // crearIngrediente
    // =====================================================

    @Test
    @DisplayName("crearIngrediente: happy path guarda el ingrediente")
    void crearIngrediente_happyPath() {
        IngredienteDTO dto = IngredienteTestData.crearDTOValido();
        when(ingredienteRepository.existsByNombreAndEmpresaIdAndActivoTrue("Nuevo Ingrediente", 1L))
                .thenReturn(false);
        when(ingredienteMapper.toEntity(dto)).thenReturn(new Ingrediente());
        when(ingredienteRepository.save(any(Ingrediente.class)))
                .thenAnswer(inv -> {
                    Ingrediente i = inv.getArgument(0);
                    i.setId(99L);
                    return i;
                });
        when(ingredienteMapper.toDTO(any(Ingrediente.class)))
                .thenReturn(IngredienteTestData.crearDTOResponse(99L));

        IngredienteDTO result = service.crearIngrediente(dto);

        assertNotNull(result);
        assertEquals(99L, result.getId());
    }

    @Test
    @DisplayName("crearIngrediente: nombre duplicado -> BusinessException")
    void crearIngrediente_nombreDuplicado_lanzaBusiness() {
        IngredienteDTO dto = IngredienteTestData.crearDTOValido();
        when(ingredienteRepository.existsByNombreAndEmpresaIdAndActivoTrue("Nuevo Ingrediente", 1L))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> service.crearIngrediente(dto));
    }

    @Test
    @DisplayName("crearIngrediente: empresaId distinto al tenant -> BusinessException")
    void crearIngrediente_empresaIdDistinto_lanzaBusiness() {
        IngredienteDTO dto = IngredienteTestData.crearDTOConEmpresaId(999L);

        assertThrows(BusinessException.class, () -> service.crearIngrediente(dto));
    }

    // =====================================================
    // actualizarIngrediente
    // =====================================================

    @Test
    @DisplayName("actualizarIngrediente: happy path actualiza datos")
    void actualizarIngrediente_happyPath() {
        Ingrediente existente = IngredienteTestData.crearIngrediente(1L, "Viejo");
        IngredienteDTO cambios = IngredienteTestData.crearDTOValido();
        when(ingredienteRepository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.of(existente));
        when(ingredienteRepository.save(any())).thenReturn(existente);
        when(ingredienteMapper.toDTO(existente))
                .thenReturn(IngredienteTestData.crearDTOResponse(1L));

        IngredienteDTO result = service.actualizarIngrediente(1L, cambios);

        assertNotNull(result);
        verify(ingredienteMapper).updateEntity(existente, cambios);
    }

    @Test
    @DisplayName("actualizarIngrediente: cambiar consumible -> BusinessException (inmutable)")
    void actualizarIngrediente_cambiarConsumible_lanzaBusiness() {
        Ingrediente existente = IngredienteTestData.crearIngrediente(1L, "Viejo");
        existente.setConsumible(false);
        IngredienteDTO cambios = IngredienteTestData.crearDTOValido();
        cambios.setConsumible(true);
        when(ingredienteRepository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class,
                () -> service.actualizarIngrediente(1L, cambios));
    }

    @Test
    @DisplayName("actualizarIngrediente: otro tenant -> 404 (IDOR)")
    void actualizarIngrediente_otroTenant_lanza404() {
        TenantContext.setEmpresaId(999L);
        IngredienteDTO cambios = IngredienteTestData.crearDTOValido();
        when(ingredienteRepository.findByIdAndEmpresaIdAndActivoTrue(1L, 999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.actualizarIngrediente(1L, cambios));
    }

    @Test
    @DisplayName("actualizarIngrediente: nombre nuevo duplicado -> BusinessException")
    void actualizarIngrediente_nombreDuplicado_lanzaBusiness() {
        Ingrediente existente = IngredienteTestData.crearIngrediente(1L, "Viejo");
        IngredienteDTO cambios = IngredienteTestData.crearDTOValido();
        cambios.setNombre("Nombre Nuevo");
        when(ingredienteRepository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.of(existente));
        when(ingredienteRepository.existsByNombreAndEmpresaIdAndActivoTrue("Nombre Nuevo", 1L))
                .thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.actualizarIngrediente(1L, cambios));
    }

    // =====================================================
    // eliminarIngrediente
    // =====================================================

    @Test
    @DisplayName("eliminarIngrediente: ingrediente no usado -> desactiva (soft delete)")
    void eliminarIngrediente_noUsadoEnRecetas_desactiva() {
        Ingrediente existente = IngredienteTestData.crearIngrediente(1L, "Cebolla");
        when(ingredienteRepository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.of(existente));
        when(recetaDetalleRepository.existsByIngredienteId(1L)).thenReturn(false);

        service.eliminarIngrediente(1L);

        assertFalse(existente.getActivo());
        verify(ingredienteRepository).save(existente);
    }

    @Test
    @DisplayName("eliminarIngrediente: ingrediente en uso en receta -> BusinessException")
    void eliminarIngrediente_enUsoEnReceta_lanzaBusiness() {
        Ingrediente existente = IngredienteTestData.crearIngrediente(1L, "Cebolla");
        when(ingredienteRepository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.of(existente));
        when(recetaDetalleRepository.existsByIngredienteId(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.eliminarIngrediente(1L));
        verify(ingredienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("eliminarIngrediente: otro tenant -> 404 (IDOR)")
    void eliminarIngrediente_otroTenant_lanza404() {
        TenantContext.setEmpresaId(999L);
        when(ingredienteRepository.findByIdAndEmpresaIdAndActivoTrue(1L, 999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.eliminarIngrediente(1L));
    }

    // =====================================================
    // contarIngredientesPorEmpresa
    // =====================================================

    @Test
    @DisplayName("contarIngredientesPorEmpresa: retorna cantidad de ingredientes activos")
    void contarIngredientesPorEmpresa_retornaConteo() {
        when(ingredienteRepository.findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(1L))
                .thenReturn(List.of(new Ingrediente(), new Ingrediente(), new Ingrediente()));

        long result = service.contarIngredientesPorEmpresa(1L);

        assertEquals(3L, result);
    }
}
