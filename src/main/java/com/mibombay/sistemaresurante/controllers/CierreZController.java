package com.mibombay.sistemaresurante.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mibombay.sistemaresurante.DTO.ReporteXDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.models.CuadreCaja;
import com.mibombay.sistemaresurante.models.Empresa;
import com.mibombay.sistemaresurante.repositories.EmpresaRepository;
import com.mibombay.sistemaresurante.security.CustomUserDetails;
import com.mibombay.sistemaresurante.services.CierreZService;
import com.mibombay.sistemaresurante.services.CuadreCajaService;
import com.mibombay.sistemaresurante.services.ReportePdfService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
public class CierreZController {

    private final CierreZService cierreZService;
    private final ReportePdfService reportePdfService;
    private final CuadreCajaService cuadreCajaService;
    private final ObjectMapper objectMapper;
    private final EmpresaRepository empresaRepository;

    public CierreZController(CierreZService cierreZService,
                              ReportePdfService reportePdfService,
                              CuadreCajaService cuadreCajaService,
                              ObjectMapper objectMapper,
                              EmpresaRepository empresaRepository) {
        this.cierreZService = cierreZService;
        this.reportePdfService = reportePdfService;
        this.cuadreCajaService = cuadreCajaService;
        this.objectMapper = objectMapper;
        this.empresaRepository = empresaRepository;
    }

    @GetMapping("/cierzx")
    public String reporteX(Model model) {
        ReporteXDTO reporte = cierreZService.obtenerReporteX();
        model.addAttribute("reporte", reporte);
        return "cierzx/reporte";
    }

    @GetMapping("/cierrez")
    @PreAuthorize("hasRole('ADMIN')")
    public String verCierreZ(Model model) {
        ReporteXDTO reporte = cierreZService.obtenerCierreZ();
        model.addAttribute("reporte", reporte);
        return "cierrez/cierre";
    }

    @GetMapping("/admin/cuadreCaja")
    @PreAuthorize("hasRole('ADMIN')")
    public String cuadreCaja(Model model, @AuthenticationPrincipal CustomUserDetails user) {
        model.addAttribute("reporte", cierreZService.obtenerReporteX());
        model.addAttribute("fecha", LocalDate.now());
        model.addAttribute("auditorNombre", user.getNombre());
        return "admin/cuadreCaja";
    }

    @PostMapping("/cierrez/cerrar")
    @PreAuthorize("hasRole('ADMIN')")
    public String cerrar(RedirectAttributes redirect,
                          @AuthenticationPrincipal CustomUserDetails user) {
        try {
            cierreZService.realizarCierreZ(user.getId());
            redirect.addFlashAttribute("success", "Cierre Z realizado correctamente");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cierrez";
    }

    // --- Cuadre de Caja: Guardar solo (sin PDF) ---

    @PostMapping("/admin/cuadreCaja/guardar")
    @PreAuthorize("hasRole('ADMIN')")
    public String guardarCuadre(@RequestParam Map<String, String> params,
                                 @AuthenticationPrincipal CustomUserDetails user,
                                 RedirectAttributes redirect) {
        ReporteXDTO reporte = cierreZService.obtenerReporteX();
        cuadreCajaService.guardar(params, reporte, user.getId());
        redirect.addFlashAttribute("success", "Cuadre de caja guardado correctamente");
        return "redirect:/admin/cuadreCaja/historial";
    }

    // --- Cuadre de Caja: Generar PDF (y guardar) ---

    @PostMapping("/admin/cuadreCaja/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> cuadreCajaPdf(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal CustomUserDetails user) {

        ReporteXDTO reporte = cierreZService.obtenerReporteX();

        Map<String, Object> datos = buildCuadreData(params, reporte, user);

        byte[] pdf = reportePdfService.generarPdf("pdf/reporte", datos);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "cuadre_caja.pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // --- Historial de cuadres ---

    @GetMapping("/admin/cuadreCaja/historial")
    @PreAuthorize("hasRole('ADMIN')")
    public String historialCuadres(Model model) {
        List<CuadreCaja> cuadres = cuadreCajaService.listar();
        model.addAttribute("cuadres", cuadres);
        return "admin/cuadreCajaHistorial";
    }

    // --- Ver detalle de un cuadre (read-only) ---

    @GetMapping("/admin/cuadreCaja/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String verCuadre(@PathVariable Long id, Model model) {
        CuadreCaja cuadre = cuadreCajaService.obtenerPorId(id);
        model.addAttribute("cuadre", cuadre);

        List<Map<String, Object>> denominaciones = parseDenominaciones(cuadre.getDenominacionesJson());
        model.addAttribute("denominaciones", denominaciones);

        return "admin/cuadreCajaDetalle";
    }

    // --- Reimprimir PDF desde un cuadre guardado ---

    @GetMapping("/admin/cuadreCaja/{id}/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> reimprimirCuadrePdf(@PathVariable Long id) {
        CuadreCaja cuadre = cuadreCajaService.obtenerPorId(id);

        Map<String, Object> datos = buildCuadreDataFromEntity(cuadre);

        byte[] pdf = reportePdfService.generarPdf("pdf/reporte", datos);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "cuadre_caja_" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // --- PDF endpoints (Reporte X / Cierre Z) ---

    @GetMapping("/cierzx/pdf")
    public ResponseEntity<byte[]> reporteXPdf() {
        ReporteXDTO reporte = cierreZService.obtenerReporteX();
        Map<String, Object> datos = new HashMap<>();
        datos.put("reporte", reporte);
        datos.put("tipo", "REPORTE_X");
        datos.put("empresaNombre", obtenerNombreEmpresa());

        byte[] pdf = reportePdfService.generarPdf("pdf/reporte", datos);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "reporte_x.pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/cierrez/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> cierreZPdf() {
        ReporteXDTO reporte = cierreZService.obtenerCierreZ();
        Map<String, Object> datos = new HashMap<>();
        datos.put("reporte", reporte);
        datos.put("tipo", "CIERRE_Z");
        datos.put("empresaNombre", obtenerNombreEmpresa());

        byte[] pdf = reportePdfService.generarPdf("pdf/reporte", datos);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "cierre_z.pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // --- Helper methods ---

    private String obtenerNombreEmpresa() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "Mi Bombay";
        return empresaRepository.findById(empresaId)
                .map(Empresa::getNombre)
                .orElse("Mi Bombay");
    }

    private Map<String, Object> buildCuadreData(Map<String, String> params, ReporteXDTO reporte, CustomUserDetails user) {
        BigDecimal subTotalDenominaciones = BigDecimal.ZERO;
        List<Map<String, Object>> denominaciones = new ArrayList<>();

        int[] billeteValores = {100000, 50000, 20000, 10000, 5000, 2000, 1000};
        for (int v : billeteValores) {
            int cant = parseIntParam(params, "billete" + v);
            Map<String, Object> d = new HashMap<>();
            d.put("tipo", "Billete");
            d.put("valor", v);
            d.put("cantidad", cant);
            d.put("subtotal", BigDecimal.valueOf(v * (long) cant));
            denominaciones.add(d);
            subTotalDenominaciones = subTotalDenominaciones.add(BigDecimal.valueOf(v * (long) cant));
        }

        int[] monedaValores = {1000, 500, 200, 100, 50};
        for (int v : monedaValores) {
            int cant = parseIntParam(params, "moneda" + v);
            Map<String, Object> d = new HashMap<>();
            d.put("tipo", "Moneda");
            d.put("valor", v);
            d.put("cantidad", cant);
            d.put("subtotal", BigDecimal.valueOf(v * (long) cant));
            denominaciones.add(d);
            subTotalDenominaciones = subTotalDenominaciones.add(BigDecimal.valueOf(v * (long) cant));
        }

        BigDecimal transferenciaTotal = parseBigDecimalParam(params, "transferencia");
        BigDecimal otrosTotal = parseBigDecimalParam(params, "otros");
        String observaciones = params.getOrDefault("observaciones", "");
        BigDecimal totalContado = subTotalDenominaciones.add(transferenciaTotal).add(otrosTotal);
        BigDecimal diferencia = totalContado.subtract(reporte.getTotalVentas());

        Map<String, Object> datos = new HashMap<>();
        datos.put("reporte", reporte);
        datos.put("tipo", "CUADRE_CAJA");
        datos.put("auditorNombre", params.getOrDefault("auditor", user.getNombre()));
        datos.put("cajero", params.getOrDefault("cajero", ""));
        datos.put("turno", params.getOrDefault("turno", ""));
        datos.put("denominaciones", denominaciones);
        datos.put("subTotalDenominaciones", subTotalDenominaciones);
        datos.put("transferenciaTotal", transferenciaTotal);
        datos.put("otrosTotal", otrosTotal);
        datos.put("totalContado", totalContado);
        datos.put("diferencia", diferencia);
        datos.put("observaciones", observaciones);
        datos.put("empresaNombre", obtenerNombreEmpresa());
        return datos;
    }

    private Map<String, Object> buildCuadreDataFromEntity(CuadreCaja cuadre) {
        ReporteXDTO reporte = ReporteXDTO.builder()
                .fecha(cuadre.getFecha())
                .cantidadVentas(cuadre.getCantidadVentasSnapshot())
                .totalVentas(cuadre.getTotalVentasSnapshot())
                .totalEfectivo(cuadre.getTotalEfectivoSnapshot())
                .totalTransferencia(cuadre.getTotalTransferenciaSnapshot())
                .build();

        List<Map<String, Object>> denominaciones = parseDenominaciones(cuadre.getDenominacionesJson());

        Map<String, Object> datos = new HashMap<>();
        datos.put("reporte", reporte);
        datos.put("tipo", "CUADRE_CAJA");
        datos.put("auditorNombre", cuadre.getAuditor());
        datos.put("cajero", cuadre.getCajero());
        datos.put("turno", cuadre.getTurno());
        datos.put("denominaciones", denominaciones);
        datos.put("subTotalDenominaciones", cuadre.getSubtotalEfectivo());
        datos.put("transferenciaTotal", cuadre.getTransferencia());
        datos.put("otrosTotal", cuadre.getOtros());
        datos.put("totalContado", cuadre.getTotalContado());
        datos.put("diferencia", cuadre.getDiferencia());
        datos.put("observaciones", cuadre.getObservaciones() != null ? cuadre.getObservaciones() : "");
        datos.put("empresaNombre", obtenerNombreEmpresa());
        return datos;
    }

    private List<Map<String, Object>> parseDenominaciones(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
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
