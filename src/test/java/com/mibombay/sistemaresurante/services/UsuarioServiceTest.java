package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.request.UsuarioRequest;
import com.mibombay.sistemaresurante.DTO.response.UsuarioResponse;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.mapper.UsuarioMapper;
import com.mibombay.sistemaresurante.models.Usuario;
import com.mibombay.sistemaresurante.repositories.UsuarioRepository;
import com.mibombay.sistemaresurante.testdata.UsuarioTestData;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de UsuarioService con Mockito.
 *
 * Patron de cada test (3 pasos):
 *   1) preparar:  configurar mocks (when...)
 *   2) ejecutar:  llamar al metodo del service
 *   3) verificar: assertEquals / assertThrows / verify
 *
 * Los datos fake vienen de UsuarioTestData (clase aparte).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - Tests unitarios")
class UsuarioServiceTest {

    @Mock private UsuarioRepository repository;
    @Mock private UsuarioMapper mapper;
    @Mock private PasswordEncoder encoder;
    @InjectMocks private UsuarioService service;

    @BeforeEach
    void setUp() {
        TenantContext.setEmpresaId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // =====================================================
    // listarUsuariosPorEmpresa
    // =====================================================

    @Test
    @DisplayName("listarUsuariosPorEmpresa: encuentra usuarios y los mapea")
    void listarUsuariosPorEmpresa_happyPath() {
        Usuario u1 = UsuarioTestData.crearUsuario(1L, "admin");
        Usuario u2 = UsuarioTestData.crearUsuario(2L, "cajero");
        when(repository.findAllByEmpresaIdAndActivoTrue(1L))
                .thenReturn(List.of(u1, u2));
        when(mapper.toResponse(u1)).thenReturn(UsuarioTestData.crearResponse(1L));
        when(mapper.toResponse(u2)).thenReturn(UsuarioTestData.crearResponse(2L));

        List<UsuarioResponse> result = service.listarUsuariosPorEmpresa(1L);

        assertEquals(2, result.size());
        verify(repository).findAllByEmpresaIdAndActivoTrue(1L);
    }

    @Test
    @DisplayName("listarUsuariosPorEmpresa: lista vacia retorna lista vacia")
    void listarUsuariosPorEmpresa_sinUsuarios() {
        when(repository.findAllByEmpresaIdAndActivoTrue(1L))
                .thenReturn(List.of());

        List<UsuarioResponse> result = service.listarUsuariosPorEmpresa(1L);

        assertTrue(result.isEmpty());
    }

    // =====================================================
    // obtenerUsuarioPorId
    // =====================================================

    @Test
    @DisplayName("obtenerUsuarioPorId: usuario existe en el mismo tenant")
    void obtenerUsuarioPorId_mismoTenant() {
        Usuario u = UsuarioTestData.crearUsuario(1L, "admin");
        when(repository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.of(u));
        when(mapper.toResponse(u)).thenReturn(UsuarioTestData.crearResponse(1L));

        UsuarioResponse result = service.obtenerUsuarioPorId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("obtenerUsuarioPorId: usuario de otro tenant -> 404 (IDOR)")
    void obtenerUsuarioPorId_otroTenant_lanza404() {
        TenantContext.setEmpresaId(999L);
        when(repository.findByIdAndEmpresaIdAndActivoTrue(1L, 999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerUsuarioPorId(1L));
    }

    // =====================================================
    // crearUsuario
    // =====================================================

    @Test
    @DisplayName("crearUsuario: happy path encripta password y guarda")
    void crearUsuario_happyPath() {
        UsuarioRequest request = UsuarioTestData.crearRequestValido();
        when(repository.existsByUsernameAndEmpresaId("nuevo_user", 1L))
                .thenReturn(false);
        when(encoder.encode("123456")).thenReturn("hashed_123456");
        when(mapper.toEntity(request)).thenReturn(new Usuario());
        when(repository.save(any(Usuario.class)))
                .thenAnswer(inv -> {
                    Usuario u = inv.getArgument(0);
                    u.setId(99L);
                    return u;
                });
        when(mapper.toResponse(any(Usuario.class)))
                .thenReturn(UsuarioTestData.crearResponse(99L));

        UsuarioResponse result = service.crearUsuario(request);

        assertNotNull(result);
        assertEquals(99L, result.getId());
        verify(encoder).encode("123456");
    }

    @Test
    @DisplayName("crearUsuario: username duplicado -> BusinessException")
    void crearUsuario_usernameDuplicado() {
        UsuarioRequest request = UsuarioTestData.crearRequestConUsername("duplicado");
        when(repository.existsByUsernameAndEmpresaId("duplicado", 1L))
                .thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.crearUsuario(request));
    }

    @Test
    @DisplayName("crearUsuario: empresaId distinto al tenant -> BusinessException")
    void crearUsuario_empresaIdDistinto() {
        UsuarioRequest request = UsuarioTestData.crearRequestConEmpresaId(999L);

        assertThrows(BusinessException.class,
                () -> service.crearUsuario(request));
    }

    // =====================================================
    // actualizarUsuario
    // =====================================================

    @Test
    @DisplayName("actualizarUsuario: happy path actualiza datos")
    void actualizarUsuario_happyPath() {
        Usuario existente = UsuarioTestData.crearUsuario(1L, "admin");
        UsuarioRequest cambios = UsuarioTestData.crearRequestValido();
        cambios.setNombre("Nuevo Nombre");
        when(repository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.of(existente));
        when(repository.save(any(Usuario.class))).thenReturn(existente);
        when(mapper.toResponse(existente)).thenReturn(UsuarioTestData.crearResponse(1L));

        UsuarioResponse result = service.actualizarUsuario(1L, cambios);

        assertNotNull(result);
        verify(mapper).updateEntity(existente, cambios);
    }

    @Test
    @DisplayName("actualizarUsuario: password vacio no se encripta")
    void actualizarUsuario_passwordVacio() {
        Usuario existente = UsuarioTestData.crearUsuario(1L, "admin");
        UsuarioRequest cambios = UsuarioTestData.crearRequestValido();
        cambios.setPassword("");
        when(repository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.of(existente));
        when(repository.save(any(Usuario.class))).thenReturn(existente);
        when(mapper.toResponse(existente)).thenReturn(UsuarioTestData.crearResponse(1L));

        service.actualizarUsuario(1L, cambios);

        verify(encoder, never()).encode(any());
    }

    @Test
    @DisplayName("actualizarUsuario: cambiar empresaId -> BusinessException")
    void actualizarUsuario_cambiarEmpresaId() {
        Usuario existente = UsuarioTestData.crearUsuario(1L, "admin");
        UsuarioRequest cambios = UsuarioTestData.crearRequestConEmpresaId(999L);
        when(repository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class,
                () -> service.actualizarUsuario(1L, cambios));
    }

    @Test
    @DisplayName("actualizarUsuario: no existe -> 404")
    void actualizarUsuario_noExiste() {
        UsuarioRequest cambios = UsuarioTestData.crearRequestValido();
        when(repository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.actualizarUsuario(1L, cambios));
    }

    // =====================================================
    // eliminarUsuario
    // =====================================================

    @Test
    @DisplayName("eliminarUsuario: desactiva (soft delete) y guarda")
    void eliminarUsuario_happyPath() {
        Usuario existente = UsuarioTestData.crearUsuario(1L, "admin");
        when(repository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.of(existente));

        service.eliminarUsuario(1L);

        assertFalse(existente.isActivo());
        verify(repository).save(existente);
    }

    @Test
    @DisplayName("eliminarUsuario: no existe -> 404")
    void eliminarUsuario_noExiste() {
        when(repository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.eliminarUsuario(1L));
    }

    // =====================================================
    // activarUsuario
    // =====================================================

    @Test
    @DisplayName("activarUsuario: reactiva usuario desactivado")
    void activarUsuario_happyPath() {
        Usuario existente = UsuarioTestData.crearUsuarioInactivo(1L, "admin");
        when(repository.findByIdAndEmpresaIdAndActivoTrue(1L, 1L))
                .thenReturn(Optional.of(existente));

        service.activarUsuario(1L);

        assertTrue(existente.isActivo());
        verify(repository).save(existente);
    }

    // =====================================================
    // contar
    // =====================================================

    @Test
    @DisplayName("contarUsuariosPorEmpresa: retorna conteo")
    void contarUsuariosPorEmpresa() {
        when(repository.countByEmpresaId(1L)).thenReturn(5L);

        assertEquals(5L, service.contarUsuariosPorEmpresa(1L));
    }

    @Test
    @DisplayName("contarTodosGlobalUsuarios: retorna conteo global")
    void contarTodosGlobalUsuarios() {
        when(repository.count()).thenReturn(42L);

        assertEquals(42L, service.contarTodosGlobalUsuarios());
    }
}
