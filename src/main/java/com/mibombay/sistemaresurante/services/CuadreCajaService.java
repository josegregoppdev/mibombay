package com.mibombay.sistemaresurante.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mibombay.sistemaresurante.DTO.ReporteXDTO;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.models.CuadreCaja;
import com.mibombay.sistemaresurante.repositories.CuadreCajaRepository;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CuadreCajaService {

    private final CuadreCajaRepository cuadreCajaRepository;
    private final ObjectMapper objectMapper;

    public CuadreCajaService(CuadreCajaRepository cuadreCajaRepository, ObjectMapper objectMapper) {
        this.cuadreCajaRepository = cuadreCajaRepository;
        this.objectMapper = objectMapper;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CuadreCaja guardar(Map<String, String> params, ReporteXDTO reporte, Long usuarioId) {
        Long empresaId = TenantContext.getEmpresaId();

        List<Map<String, Object>> denominaciones = new java.util.ArrayList<>();
        BigDecimal subTotalEfectivo = BigDecimal.ZERO;

        int[] billeteValores = {100000, 50000, 20000, 10000, 5000, 2000, 1000};
        for (int v : billeteValores) {
            int cant = parseIntParam(params, "billete" + v);
            Map<String, Object> d = new java.util.HashMap<>();
            d.put("tipo", "Billete");
            d.put("valor", v);
            d.put("cantidad", cant);
            d.put("subtotal", v * (long) cant);
            denominaciones.add(d);
            subTotalEfectivo = subTotalEfectivo.add(BigDecimal.valueOf(v * (long) cant));
        }

        int[] monedaValores = {1000, 500, 200, 100, 50};
        for (int v : monedaValores) {
            int cant = parseIntParam(params, "moneda" + v);
            Map<String, Object> d = new java.util.HashMap<>();
            d.put("tipo", "Moneda");
            d.put("valor", v);
            d.put("cantidad", cant);
            d.put("subtotal", v * (long) cant);
            denominaciones.add(d);
            subTotalEfectivo = subTotalEfectivo.add(BigDecimal.valueOf(v * (long) cant));
        }

        BigDecimal transferencia = parseBigDecimalParam(params, "transferencia");
        BigDecimal otros = parseBigDecimalParam(params, "otros");
        BigDecimal totalContado = subTotalEfectivo.add(transferencia).add(otros);
        BigDecimal diferencia = totalContado.subtract(reporte.getTotalVentas());

        String denominacionesJson;
        try {
            denominacionesJson = objectMapper.writeValueAsString(denominaciones);
        } catch (JsonProcessingException e) {
            denominacionesJson = "[]";
        }

        CuadreCaja cuadre = CuadreCaja.builder()
                .empresaId(empresaId)
                .usuarioId(usuarioId)
                .fecha(LocalDate.now())
                .auditor(params.getOrDefault("auditor", ""))
                .cajero(params.getOrDefault("cajero", ""))
                .turno(params.getOrDefault("turno", ""))
                .denominacionesJson(denominacionesJson)
                .subtotalEfectivo(subTotalEfectivo)
                .transferencia(transferencia)
                .otros(otros)
                .totalContado(totalContado)
                .totalSistema(reporte.getTotalVentas())
                .diferencia(diferencia)
                .cantidadVentasSnapshot(reporte.getCantidadVentas())
                .totalVentasSnapshot(reporte.getTotalVentas())
                .totalEfectivoSnapshot(reporte.getTotalEfectivo())
                .totalTransferenciaSnapshot(reporte.getTotalTransferencia())
                .observaciones(params.getOrDefault("observaciones", ""))
                .build();

        return cuadreCajaRepository.save(cuadre);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<CuadreCaja> listar() {
        Long empresaId = TenantContext.getEmpresaId();
        return cuadreCajaRepository.findByEmpresaIdAndActivoTrueOrderByFechaCreacionDesc(empresaId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public CuadreCaja obtenerPorId(Long id) {
        Long empresaId = TenantContext.getEmpresaId();
        return cuadreCajaRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuadre de caja no encontrado"));
    }

    private int parseIntParam(Map<String, String> params, String key) {
        String val = params.get(key);
        if (val == null || val.isBlank()) return 0;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return 0; }
    }

    private BigDecimal parseBigDecimalParam(Map<String, String> params, String key) {
        String val = params.get(key);
        if (val == null || val.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(val.replace(",", "")); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
}
