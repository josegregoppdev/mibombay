package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.ConsumoReporteDTO;
import com.mibombay.sistemaresurante.DTO.ConsumoResumenDTO;
import com.mibombay.sistemaresurante.services.ConsumoExcelService;
import com.mibombay.sistemaresurante.services.FoodCostService;
import com.mibombay.sistemaresurante.services.ReporteService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reportes")
@PreAuthorize("hasRole('ADMIN')")
public class ReporteController {

    private final ReporteService reporteService;
    private final ConsumoExcelService excelService;
    private final FoodCostService foodCostService;

    public ReporteController(ReporteService reporteService,
                             ConsumoExcelService excelService,
                             FoodCostService foodCostService) {
        this.reporteService = reporteService;
        this.excelService = excelService;
        this.foodCostService = foodCostService;
    }

    @GetMapping("/consumo")
    public String consumo(@RequestParam(required = false) LocalDate desde,
                          @RequestParam(required = false) LocalDate hasta,
                          Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "redirect:/login";

        if (desde == null) desde = LocalDate.now().minusDays(7);
        if (hasta == null) hasta = LocalDate.now();

        List<ConsumoReporteDTO> reporte = reporteService.generarReporteConsumo(desde, hasta, empresaId);
        ConsumoResumenDTO resumen = foodCostService.calcularResumenConsumo(desde, hasta, empresaId);

        model.addAttribute("reporte", reporte);
        model.addAttribute("resumen", resumen);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        return "reportes/consumo";
    }

    @GetMapping("/consumo/excel")
    public void exportarExcel(@RequestParam(required = false) LocalDate desde,
                              @RequestParam(required = false) LocalDate hasta,
                              HttpServletResponse response) throws Exception {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return;

        if (desde == null) desde = LocalDate.now().minusDays(7);
        if (hasta == null) hasta = LocalDate.now();

        List<ConsumoReporteDTO> reporte = reporteService.generarReporteConsumo(desde, hasta, empresaId);
        ConsumoResumenDTO resumen = foodCostService.calcularResumenConsumo(desde, hasta, empresaId);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = "consumo_" + desde.toString() + "_" + hasta.toString() + ".xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        byte[] bytes = excelService.generar(reporte, desde, hasta, empresaId, resumen);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }
}
