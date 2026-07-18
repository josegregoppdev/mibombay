package com.mibombay.sistemaresurante.security.owasp;

import com.mibombay.sistemaresurante.services.ReporteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests OWASP A01 - Broken Access Control
 *
 * Verifica que ReporteService.generarReporteConsumo() solo es accesible por ADMIN.
 * CAJERO y usuarios no autenticados deben ser rechazados.
 */
@SpringBootTest
@DisplayName("OWASP A01: ReporteService @PreAuthorize")
class ReporteServiceSecurityTest {

    @Autowired
    private ReporteService service;

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    @DisplayName("ADMIN puede generar reporte de consumo")
    void adminPuedeGenerarReporte() {
        // Reporte de hoy, empresa 1
        assertDoesNotThrow(() -> service.generarReporteConsumo(
            LocalDate.now(), LocalDate.now(), 1L));
    }

    @Test
    @WithMockUser(roles = "CAJERO", username = "cajero@test.com")
    @DisplayName("CAJERO NO puede generar reporte de consumo")
    void cajeroNoPuedeGenerarReporte() {
        assertThrows(AccessDeniedException.class,
            () -> service.generarReporteConsumo(LocalDate.now(), LocalDate.now(), 1L));
    }

    @Test
    @DisplayName("Usuario no autenticado NO puede generar reporte")
    void usuarioNoAutenticadoNoPuedeGenerar() {
        assertThrows(AccessDeniedException.class,
            () -> service.generarReporteConsumo(LocalDate.now(), LocalDate.now(), 1L));
    }
}
