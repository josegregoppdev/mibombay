package com.mibombay.sistemaresurante.security.owasp;

import com.mibombay.sistemaresurante.services.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests OWASP A01 - Broken Access Control
 *
 * Verifica que UsuarioService solo es accesible por ADMIN.
 * CAJERO y SUPERADMIN (sin empresa) deben ser rechazados.
 *
 * Estrategia:
 * - ADMIN llega al cuerpo del método (puede lanzar ResourceNotFoundException si el ID no existe)
 * - CAJERO es bloqueado por @PreAuthorize (lanza AccessDeniedException)
 * - Sin auth es bloqueado (lanza AccessDeniedException)
 */
@SpringBootTest
@DisplayName("OWASP A01: UsuarioService @PreAuthorize")
class UsuarioServiceSecurityTest {

    @Autowired
    private UsuarioService usuarioService;

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    @DisplayName("ADMIN puede listar usuarios por empresa")
    void adminPuedeListarUsuarios() {
        // ADMIN llega al cuerpo del método - usar empresa 1 que existe en DataInitializer
        assertDoesNotThrow(() -> usuarioService.listarPorEmpresa(1L));
    }

    @Test
    @WithMockUser(roles = "CAJERO", username = "cajero@test.com")
    @DisplayName("CAJERO NO puede listar usuarios por empresa")
    void cajeroNoPuedeListarUsuarios() {
        // CAJERO es bloqueado por @PreAuthorize
        assertThrows(AccessDeniedException.class, () -> usuarioService.listarPorEmpresa(1L));
    }

    @Test
    @DisplayName("Usuario no autenticado NO puede listar usuarios")
    void usuarioNoAutenticadoNoPuedeListar() {
        // Sin autenticación = bloqueado
        assertThrows(AccessDeniedException.class, () -> usuarioService.listarPorEmpresa(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    @DisplayName("ADMIN puede contar usuarios totales")
    void adminPuedeContarUsuarios() {
        // Método simple que retorna long - no requiere datos
        assertDoesNotThrow(() -> usuarioService.contarTotal());
    }

    @Test
    @WithMockUser(roles = "CAJERO", username = "cajero@test.com")
    @DisplayName("CAJERO NO puede contar usuarios totales")
    void cajeroNoPuedeContarUsuarios() {
        assertThrows(AccessDeniedException.class, () -> usuarioService.contarTotal());
    }
}
