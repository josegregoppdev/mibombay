package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.FoodCostDiarioDTO;
import com.mibombay.sistemaresurante.DTO.FoodCostItemDTO;
import com.mibombay.sistemaresurante.DTO.FoodCostResumenDTO;
import com.mibombay.sistemaresurante.models.CostoComidaDiaria;
import com.mibombay.sistemaresurante.services.FoodCostExcelService;
import com.mibombay.sistemaresurante.services.FoodCostService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class FoodCostController {

    private final FoodCostService foodCostService;
    private final FoodCostExcelService excelService;

    public FoodCostController(FoodCostService foodCostService, FoodCostExcelService excelService) {
        this.foodCostService = foodCostService;
        this.excelService = excelService;
    }

    @GetMapping("/food-cost")
    public String foodCost(Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "redirect:/login";

        LocalDate fecha = LocalDate.now();

        Optional<CostoComidaDiaria> guardado = foodCostService.obtenerCostoComidaDiaria(empresaId, fecha);
        boolean existeDiaSiguiente = foodCostService.existeDiaSiguienteGuardado(empresaId, fecha);
        boolean existeGuardado = guardado.isPresent();
        boolean puedeEditar = existeGuardado && !existeDiaSiguiente;

        FoodCostDiarioDTO data;
        boolean tieneDatosParaGuardar;
        boolean itemsDeBD;

        if (existeGuardado) {
            CostoComidaDiaria cc = guardado.get();
            boolean esHoy = cc.getFecha().equals(LocalDate.now());

            FoodCostResumenDTO resumen = construirResumenDesdeEntidad(cc);

            // Si es hoy → items calculados en tiempo real
            // Si no es hoy → items desde BD
            List<FoodCostItemDTO> items;
            if (esHoy) {
                items = foodCostService.calcularPorItem(fecha, empresaId);
                itemsDeBD = false;
            } else {
                items = foodCostService.obtenerItemsGuardados(cc.getId());
                itemsDeBD = true;
            }

            data = FoodCostDiarioDTO.builder()
                    .resumen(resumen)
                    .items(items)
                    .categorias(new ArrayList<>())
                    .build();
            tieneDatosParaGuardar = false;
        } else {
            data = foodCostService.calcularDiario(fecha, empresaId);
            tieneDatosParaGuardar = val(data.getResumen().getVentasTotales()).compareTo(BigDecimal.ZERO) > 0;
            itemsDeBD = false;
        }

        model.addAttribute("data", data);
        model.addAttribute("fecha", fecha);
        model.addAttribute("costoGuardado", guardado.orElse(null));
        model.addAttribute("existeGuardado", existeGuardado);
        model.addAttribute("puedeEditar", puedeEditar);
        model.addAttribute("existeDiaSiguiente", existeDiaSiguiente);
        model.addAttribute("tieneDatosParaGuardar", tieneDatosParaGuardar);
        model.addAttribute("itemsDeBD", itemsDeBD);

        return "food-cost/index";
    }

    private BigDecimal val(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    @PostMapping("/food-cost/guardar")
    public String guardar(RedirectAttributes redirect) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "redirect:/login";

        foodCostService.guardarCostoComidaDiaria(LocalDate.now(), empresaId);
        redirect.addFlashAttribute("exito", "Costos del día guardados correctamente.");

        return "redirect:/admin/food-cost";
    }

    @PostMapping("/food-cost/recalcular")
    public String recalcular(RedirectAttributes redirect) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "redirect:/login";

        LocalDate fecha = LocalDate.now();

        boolean puedeEditar = foodCostService.existeCostoGuardado(empresaId, fecha)
                && !foodCostService.existeDiaSiguienteGuardado(empresaId, fecha);

        if (!puedeEditar) {
            redirect.addFlashAttribute("error", "No se puede recalcular: el día siguiente ya está guardado.");
            return "redirect:/admin/food-cost";
        }

        foodCostService.guardarCostoComidaDiaria(fecha, empresaId);
        redirect.addFlashAttribute("exito", "Costos del día recalculados correctamente.");

        return "redirect:/admin/food-cost";
    }

    @GetMapping("/food-cost/excel")
    public void exportarExcel(HttpServletResponse response) throws Exception {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return;

        LocalDate fecha = LocalDate.now();

        FoodCostDiarioDTO data;
        Optional<CostoComidaDiaria> guardado = foodCostService.obtenerCostoComidaDiaria(empresaId, fecha);

        if (guardado.isPresent()) {
            CostoComidaDiaria cc = guardado.get();
            FoodCostResumenDTO resumen = construirResumenDesdeEntidad(cc);
            List<FoodCostItemDTO> items = foodCostService.calcularPorItem(fecha, empresaId);
            data = FoodCostDiarioDTO.builder()
                    .resumen(resumen)
                    .items(items)
                    .categorias(new ArrayList<>())
                    .build();
        } else {
            data = foodCostService.calcularDiario(fecha, empresaId);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = "foodcost_" + fecha.toString() + ".xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        byte[] bytes = excelService.generar(data, empresaId);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    private FoodCostResumenDTO construirResumenDesdeEntidad(CostoComidaDiaria cc) {
        return FoodCostResumenDTO.builder()
                .fecha(cc.getFecha())
                .ventasTotales(val(cc.getVentasTotales()))
                .costoIngredientesVendidos(val(cc.getCostoIngredientesVendidos()))
                .foodCostPorcentaje(val(cc.getFoodCostPorcentaje()))
                .inventarioInicialValor(val(cc.getInventarioInicialValor()))
                .comprasValor(val(cc.getComprasValor()))
                .inventarioFinalValor(val(cc.getInventarioFinalValor()))
                .costoAlimentosContable(val(cc.getCostoAlimentosContable()))
                .foodCostContablePorcentaje(val(cc.getFoodCostContablePorcentaje()))
                .mermaValor(val(cc.getMermaValor()))
                .desperdicioValor(val(cc.getDesperdicioValor()))
                .diferenciaContable(val(cc.getDiferenciaValor()))
                .mermaPorcentaje(val(cc.getMermaPorcentaje()))
                .desperdicioPorcentaje(val(cc.getDesperdicioPorcentaje()))
                .diferenciaInventarioPorcentaje(val(cc.getDiferenciaPorcentaje()))
                .consumoIndirectoValor(val(cc.getConsumoIndirectoValor()))
                .costoRealValor(val(cc.getCostoRealValor()))
                .costoRealPorcentaje(val(cc.getCostoRealPorcentaje()))
                .build();
    }

    @GetMapping("/food-cost/historico")
    public String historico(@RequestParam(required = false) LocalDate fecha, Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "redirect:/login";

        if (fecha == null) fecha = LocalDate.now();

        Optional<CostoComidaDiaria> guardado = foodCostService.obtenerCostoComidaDiaria(empresaId, fecha);
        boolean sinDatos = guardado.isEmpty();

        FoodCostDiarioDTO data = null;

        if (!sinDatos) {
            CostoComidaDiaria cc = guardado.get();
            FoodCostResumenDTO resumen = construirResumenDesdeEntidad(cc);
            List<FoodCostItemDTO> items = foodCostService.obtenerItemsGuardados(cc.getId());

            data = FoodCostDiarioDTO.builder()
                    .resumen(resumen)
                    .items(items)
                    .categorias(new ArrayList<>())
                    .build();
        }

        model.addAttribute("data", data);
        model.addAttribute("fecha", fecha);
        model.addAttribute("sinDatos", sinDatos);

        return "food-cost/historico";
    }

    @GetMapping("/food-cost/historico/excel")
    public void exportarExcelHistorico(@RequestParam(required = false) LocalDate fecha,
                                       HttpServletResponse response) throws Exception {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return;

        if (fecha == null) fecha = LocalDate.now();

        Optional<CostoComidaDiaria> guardado = foodCostService.obtenerCostoComidaDiaria(empresaId, fecha);

        if (guardado.isPresent()) {
            CostoComidaDiaria cc = guardado.get();
            FoodCostResumenDTO resumen = construirResumenDesdeEntidad(cc);
            List<FoodCostItemDTO> items = foodCostService.obtenerItemsGuardados(cc.getId());
            FoodCostDiarioDTO data = FoodCostDiarioDTO.builder()
                    .resumen(resumen)
                    .items(items)
                    .categorias(new ArrayList<>())
                    .build();

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String filename = "foodcost_historico_" + fecha.toString() + ".xlsx";
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            byte[] bytes = excelService.generar(data, empresaId);
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } else {
            response.sendRedirect("/admin/food-cost/historico?fecha=" + fecha);
        }
    }
}
