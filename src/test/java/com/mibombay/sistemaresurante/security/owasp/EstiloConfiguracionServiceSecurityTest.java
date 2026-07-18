package com.mibombay.sistemaresurante.security.owasp;

import com.mibombay.sistemaresurante.services.EstiloConfiguracionService;
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
 * Verifica que EstiloConfiguracionService solo es accesible por ADMIN.
 * CAJERO y usuarios no autenticados deben ser rechazados.
 */
@SpringBootTest
@DisplayName("OWASP A01: EstiloConfiguracionService @PreAuthorize")
class EstiloConfiguracionServiceSecurityTest {

    @Autowired
    private EstiloConfiguracionService service;

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    @DisplayName("ADMIN puede obtener estilo por empresa")
    void adminPuedeObtenerEstilo() {
        // Optional.empty() si no existe, no falla
        assertDoesNotThrow(() -> service.obtenerPorEmpresaId(1L));
    }

    @Test
    @WithMockUser(roles = "CAJERO", username = "cajero@test.com")
    @DisplayName("CAJERO NO puede obtener estilo por empresa")
    void cajeroNoPuedeObtenerEstilo() {
        assertThrows(AccessDeniedException.class, () -> service.obtenerPorEmpresaId(1L));
    }

    @Test
    @DisplayName("Usuario no autenticado NO puede obtener estilo")
    void usuarioNoAutenticadoNoPuedeObtener() {
        assertThrows(AccessDeniedException.class, () -> service.obtenerPorEmpresaId(1L));
    }
}
