package com.mibombay.sistemaresurante.security.owasp;

import com.mibombay.sistemaresurante.services.MovimientoInventarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests OWASP A01 - Broken Access Control
 *
 * Verifica que MovimientoInventarioService.listar() solo es accesible por ADMIN.
 * CAJERO y usuarios no autenticados deben ser rechazados.
 */
@SpringBootTest
@DisplayName("OWASP A01: MovimientoInventarioService @PreAuthorize")
class MovimientoInventarioServiceSecurityTest {

    @Autowired
    private MovimientoInventarioService service;

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    @DisplayName("ADMIN puede listar movimientos de inventario")
    void adminPuedeListarMovimientos() {
        // ADMIN llega al cuerpo - usar Pageable vacío
        assertDoesNotThrow(() -> service.listar(null, null, null, PageRequest.of(0, 10)));
    }

    @Test
    @WithMockUser(roles = "CAJERO", username = "cajero@test.com")
    @DisplayName("CAJERO NO puede listar movimientos de inventario")
    void cajeroNoPuedeListarMovimientos() {
        assertThrows(AccessDeniedException.class,
            () -> service.listar(null, null, null, PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("Usuario no autenticado NO puede listar movimientos")
    void usuarioNoAutenticadoNoPuedeListar() {
        assertThrows(AccessDeniedException.class,
            () -> service.listar(null, null, null, PageRequest.of(0, 10)));
    }
}
