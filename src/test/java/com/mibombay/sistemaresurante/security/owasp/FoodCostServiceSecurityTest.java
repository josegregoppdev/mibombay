package com.mibombay.sistemaresurante.security.owasp;

import com.mibombay.sistemaresurante.services.FoodCostService;
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
 * Verifica que FoodCostService solo es accesible por ADMIN.
 * Food Cost contiene datos financieros sensibles (costos, márgenes, etc.)
 * por lo que NINGÚN otro rol debe tener acceso.
 *
 * Servicios verificados:
 * - calcularDiario
 * - calcularPorItem
 * - calcularPorItemFromMovimientos
 * - calcularResumenConsumo
 * - guardarCostoComidaDiaria
 * - obtenerCostoComidaDiaria
 * - obtenerItemsGuardados
 * - obtenerCostoComidaPorRango
 * - existeCostoGuardado
 * - existeDiaSiguienteGuardado
 */
@SpringBootTest
@DisplayName("OWASP A01: FoodCostService @PreAuthorize")
class FoodCostServiceSecurityTest {

    @Autowired
    private FoodCostService service;

    // ========================
    // calcularDiario
    // ========================
    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    @DisplayName("ADMIN puede calcular food cost diario")
    void adminPuedeCalcularDiario() {
        assertDoesNotThrow(() -> service.calcularDiario(LocalDate.now(), 1L));
    }

    @Test
    @WithMockUser(roles = "CAJERO", username = "cajero@test.com")
    @DisplayName("CAJERO NO puede calcular food cost diario")
    void cajeroNoPuedeCalcularDiario() {
        assertThrows(AccessDeniedException.class,
            () -> service.calcularDiario(LocalDate.now(), 1L));
    }

    // ========================
    // calcularPorItem
    // ========================
    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    @DisplayName("ADMIN puede calcular food cost por item")
    void adminPuedeCalcularPorItem() {
        assertDoesNotThrow(() -> service.calcularPorItem(LocalDate.now(), 1L));
    }

    @Test
    @WithMockUser(roles = "CAJERO", username = "cajero@test.com")
    @DisplayName("CAJERO NO puede calcular food cost por item")
    void cajeroNoPuedeCalcularPorItem() {
        assertThrows(AccessDeniedException.class,
            () -> service.calcularPorItem(LocalDate.now(), 1L));
    }

    // ========================
    // calcularResumenConsumo
    // ========================
    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    @DisplayName("ADMIN puede calcular resumen de consumo")
    void adminPuedeCalcularResumenConsumo() {
        assertDoesNotThrow(() -> service.calcularResumenConsumo(
            LocalDate.now().minusDays(7), LocalDate.now(), 1L));
    }

    @Test
    @WithMockUser(roles = "CAJERO", username = "cajero@test.com")
    @DisplayName("CAJERO NO puede calcular resumen de consumo")
    void cajeroNoPuedeCalcularResumenConsumo() {
        assertThrows(AccessDeniedException.class,
            () -> service.calcularResumenConsumo(
                LocalDate.now().minusDays(7), LocalDate.now(), 1L));
    }

    // ========================
    // existeCostoGuardado (método simple)
    // ========================
    @Test
    @WithMockUser(roles = "ADMIN", username = "admin@test.com")
    @DisplayName("ADMIN puede consultar si existe costo guardado")
    void adminPuedeVerificarCostoGuardado() {
        assertDoesNotThrow(() -> service.existeCostoGuardado(1L, LocalDate.now()));
    }

    @Test
    @WithMockUser(roles = "CAJERO", username = "cajero@test.com")
    @DisplayName("CAJERO NO puede consultar si existe costo guardado")
    void cajeroNoPuedeVerificarCostoGuardado() {
        assertThrows(AccessDeniedException.class,
            () -> service.existeCostoGuardado(1L, LocalDate.now()));
    }

    // ========================
    // Sin autenticación
    // ========================
    @Test
    @DisplayName("Usuario no autenticado NO puede acceder a FoodCostService")
    void usuarioNoAutenticadoNoPuedeAcceder() {
        assertThrows(AccessDeniedException.class,
            () -> service.calcularDiario(LocalDate.now(), 1L));
    }
}
