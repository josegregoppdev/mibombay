package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.ReporteXDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.models.CierreZ;
import com.mibombay.sistemaresurante.models.Venta;
import com.mibombay.sistemaresurante.repositories.CierreZRepository;
import com.mibombay.sistemaresurante.repositories.UsuarioRepository;
import com.mibombay.sistemaresurante.repositories.VentaRepository;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CierreZService {

    private final CierreZRepository cierreZRepository;
    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;

    public CierreZService(CierreZRepository cierreZRepository,
                           VentaRepository ventaRepository,
                           UsuarioRepository usuarioRepository) {
        this.cierreZRepository = cierreZRepository;
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public ReporteXDTO obtenerReporteX() {
        Long empresaId = TenantContext.getEmpresaId();
        LocalDate hoy = LocalDate.now();

        List<Venta> ventas = ventaRepository.findByEmpresaIdAndFechaVentaBetweenAndActivoTrueOrderByFechaVentaDesc(
                empresaId, hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX));

        return calcularReporte(ventas, hoy);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ReporteXDTO realizarCierreZ(Long usuarioId) {
        Long empresaId = TenantContext.getEmpresaId();
        LocalDate hoy = LocalDate.now();

        if (existeCierreZHoy()) {
            throw new BusinessException("El día de hoy ya tiene un cierre Z registrado");
        }

        List<Venta> ventas = ventaRepository.findByEmpresaIdAndFechaVentaBetweenAndActivoTrueOrderByFechaVentaDesc(
                empresaId, hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX));

        ReporteXDTO reporte = calcularReporte(ventas, hoy);

        CierreZ cierre = CierreZ.builder()
                .empresaId(empresaId)
                .usuarioId(usuarioId)
                .fecha(hoy)
                .totalVentas(reporte.getTotalVentas())
                .cantidadVentas(reporte.getCantidadVentas())
                .totalEfectivo(reporte.getTotalEfectivo())
                .totalTransferencia(reporte.getTotalTransferencia())
                .build();
        cierreZRepository.save(cierre);

        reporte.setCierreRealizado(true);
        reporte.setNombreUsuarioCierre(usuarioRepository.findById(usuarioId)
                .map(u -> u.getNombre() + (u.getApellido() != null ? " " + u.getApellido() : ""))
                .orElse("Usuario #" + usuarioId));
        reporte.setFechaCierre(cierre.getFechaCreacion());

        return reporte;
    }

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public boolean existeCierreZHoy() {
        Long empresaId = TenantContext.getEmpresaId();
        return cierreZRepository.existsByEmpresaIdAndFechaAndActivoTrue(empresaId, LocalDate.now());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ReporteXDTO obtenerCierreZ() {
        Long empresaId = TenantContext.getEmpresaId();
        LocalDate hoy = LocalDate.now();

        Optional<CierreZ> optCierre = cierreZRepository.findByEmpresaIdAndFechaAndActivoTrue(empresaId, hoy);
        if (optCierre.isEmpty()) {
            return obtenerReporteX();
        }

        CierreZ cierre = optCierre.get();
        return ReporteXDTO.builder()
                .fecha(cierre.getFecha())
                .cantidadVentas(cierre.getCantidadVentas())
                .totalVentas(cierre.getTotalVentas())
                .totalEfectivo(cierre.getTotalEfectivo())
                .totalTransferencia(cierre.getTotalTransferencia())
                .cierreRealizado(true)
                .nombreUsuarioCierre(usuarioRepository.findById(cierre.getUsuarioId())
                        .map(u -> u.getNombre() + (u.getApellido() != null ? " " + u.getApellido() : ""))
                        .orElse("Usuario #" + cierre.getUsuarioId()))
                .fechaCierre(cierre.getFechaCreacion())
                .build();
    }

    private ReporteXDTO calcularReporte(List<Venta> ventas, LocalDate fecha) {
        int cantidad = ventas.size();
        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalTransferencia = BigDecimal.ZERO;

        for (Venta v : ventas) {
            if (v.getTotal() != null) totalVentas = totalVentas.add(v.getTotal());
            BigDecimal efectivoNeto = v.getRecibidoEfectivo() != null ? v.getRecibidoEfectivo() : BigDecimal.ZERO;
            if (v.getCambio() != null) efectivoNeto = efectivoNeto.subtract(v.getCambio());
            totalEfectivo = totalEfectivo.add(efectivoNeto);
            if (v.getRecibidoTransferencia() != null) totalTransferencia = totalTransferencia.add(v.getRecibidoTransferencia());
        }

        return ReporteXDTO.builder()
                .fecha(fecha)
                .cantidadVentas(cantidad)
                .totalVentas(totalVentas)
                .totalEfectivo(totalEfectivo)
                .totalTransferencia(totalTransferencia)
                .cierreRealizado(false)
                .build();
    }
}
