package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.ConsumoPeriodoDTO;
import com.mibombay.sistemaresurante.DTO.ConsumoPeriodoFormDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.models.ConsumoPeriodo;
import com.mibombay.sistemaresurante.models.ConsumoPeriodoDetalle;
import com.mibombay.sistemaresurante.security.CustomUserDetails;
import com.mibombay.sistemaresurante.services.ConsumoPeriodoService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/consumo-periodo")
@PreAuthorize("hasRole('ADMIN')")
public class ConsumoPeriodoController {

    private final ConsumoPeriodoService service;

    public ConsumoPeriodoController(ConsumoPeriodoService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "redirect:/login";
        List<ConsumoPeriodoDTO> lista = service.listar(empresaId);
        model.addAttribute("periodos", lista);
        return "consumo-periodo/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(defaultValue = "SEMANAL") String tipo,
                        @AuthenticationPrincipal CustomUserDetails user,
                        RedirectAttributes redirect) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "redirect:/login";
        try {
            ConsumoPeriodo periodo = service.crearBorrador(tipo, user.getId(), empresaId);
            return "redirect:/admin/consumo-periodo/" + periodo.getId() + "/editar";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/consumo-periodo";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "redirect:/login";

        ConsumoPeriodo periodo = service.obtenerPorId(id);
        List<ConsumoPeriodoDetalle> detalles = service.obtenerDetalles(id);

        model.addAttribute("periodo", periodo);
        model.addAttribute("detalles", detalles);

        return "consumo-periodo/form";
    }

    @PostMapping("/{id}/guardar")
    public String guardar(@PathVariable Long id,
                          @ModelAttribute ConsumoPeriodoFormDTO form,
                          RedirectAttributes redirect) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "redirect:/login";
        try {
            service.actualizarBorrador(id, form.getItems(), empresaId);
            redirect.addFlashAttribute("success", "Borrador guardado correctamente");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/consumo-periodo/" + id + "/editar";
    }

    @PostMapping("/{id}/confirmar")
    public String confirmar(@PathVariable Long id,
                            @AuthenticationPrincipal CustomUserDetails user,
                            RedirectAttributes redirect) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "redirect:/login";
        try {
            service.confirmar(id, user.getId(), empresaId);
            redirect.addFlashAttribute("success", "Consumo confirmado y stock actualizado");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/consumo-periodo";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "redirect:/login";

        ConsumoPeriodo periodo = service.obtenerPorId(id);
        List<ConsumoPeriodoDetalle> detalles = service.obtenerDetalles(id);

        model.addAttribute("periodo", periodo);
        model.addAttribute("detalles", detalles);

        return "consumo-periodo/detalle";
    }
}
