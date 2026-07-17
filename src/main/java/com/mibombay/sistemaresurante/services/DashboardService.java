package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.DashboardStatsDTO;
import com.mibombay.sistemaresurante.repositories.ProductoRepository;
import com.mibombay.sistemaresurante.repositories.VentaRepository;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final BigDecimal STOCK_BAJO_UMBRAL = new BigDecimal("10");

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;

    public DashboardService(VentaRepository ventaRepository, ProductoRepository productoRepository) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public DashboardStatsDTO obtenerStatsDelDia() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            return DashboardStatsDTO.builder()
                    .totalVentasDia(BigDecimal.ZERO)
                    .numeroVentasDia(0)
                    .productosStockBajo(0)
                    .empresaId(null)
                    .build();
        }

        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = LocalDate.now().atTime(LocalTime.MAX);

        BigDecimal totalVentas = ventaRepository.sumTotalVentasDelDia(empresaId, inicioDia, finDia);
        int numeroVentas = ventaRepository.countVentasDelDia(empresaId, inicioDia, finDia);
        int productosStockBajo = productoRepository.countProductosStockBajo(empresaId, STOCK_BAJO_UMBRAL);

        return DashboardStatsDTO.builder()
                .totalVentasDia(totalVentas != null ? totalVentas : BigDecimal.ZERO)
                .numeroVentasDia(numeroVentas)
                .productosStockBajo(productosStockBajo)
                .empresaId(empresaId)
                .build();
    }
}
