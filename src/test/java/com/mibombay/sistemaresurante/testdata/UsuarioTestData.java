package com.mibombay.sistemaresurante.testdata;

import com.mibombay.sistemaresurante.DTO.request.UsuarioRequest;
import com.mibombay.sistemaresurante.DTO.response.UsuarioResponse;
import com.mibombay.sistemaresurante.models.Usuario;
import com.mibombay.sistemaresurante.models.enums.Rol;

/**
 * Clase con datos de prueba (Object Mother).
 * Todos los metodos son static: se llaman directo, sin new.
 *
 * Ejemplo:
 *   Usuario u = UsuarioTestData.crearUsuario(1L, "admin");
 */
public class UsuarioTestData {

    // ----- Usuario (entidad) -----

    /** Crea un Usuario fake con id, username y empresa 1L. */
    public static Usuario crearUsuario(Long id, String username) {
        return Usuario.builder()
                .id(id)
                .empresaId(1L)
                .nombre("Test")
                .apellido("User")
                .email("test@test.com")
                .telefono("3001234567")
                .username(username)
                .password("hashed_password")
                .rol(Rol.CAJERO)
                .activo(true)
                .esSuperadmin(false)
                .build();
    }

    /** Igual al anterior pero con empresaId custom. */
    public static Usuario crearUsuarioConEmpresa(Long id, String username, Long empresaId) {
        Usuario u = crearUsuario(id, username);
        u.setEmpresaId(empresaId);
        return u;
    }

    /** Crea un usuario ya desactivado (soft delete). */
    public static Usuario crearUsuarioInactivo(Long id, String username) {
        Usuario u = crearUsuario(id, username);
        u.setActivo(false);
        return u;
    }

    // ----- UsuarioRequest (DTO de entrada) -----

    /** Request con datos validos para crear usuario. */
    public static UsuarioRequest crearRequestValido() {
        return UsuarioRequest.builder()
                .nombre("Nuevo")
                .apellido("Usuario")
                .email("nuevo@test.com")
                .username("nuevo_user")
                .password("123456")
                .rol(Rol.CAJERO)
                .empresaId(1L)
                .build();
    }

    /** Request con username custom (para tests de duplicado). */
    public static UsuarioRequest crearRequestConUsername(String username) {
        UsuarioRequest r = crearRequestValido();
        r.setUsername(username);
        return r;
    }

    /** Request con empresaId custom (para tests cross-tenant). */
    public static UsuarioRequest crearRequestConEmpresaId(Long empresaId) {
        UsuarioRequest r = crearRequestValido();
        r.setEmpresaId(empresaId);
        return r;
    }

    // ----- UsuarioResponse (DTO de salida) -----

    /** Response basico con un id. */
    public static UsuarioResponse crearResponse(Long id) {
        return UsuarioResponse.builder()
                .id(id)
                .nombre("Test")
                .apellido("User")
                .username("test_user")
                .rol(Rol.CAJERO)
                .empresaId(1L)
                .activo(true)
                .build();
    }
}
