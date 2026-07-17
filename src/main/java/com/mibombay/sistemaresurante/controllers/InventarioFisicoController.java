package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.InventarioFisicoFormDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.models.InventarioFisico;
import com.mibombay.sistemaresurante.models.InventarioFisicoDetalle;
import com.mibombay.sistemaresurante.models.enums.InventarioEstado;
import com.mibombay.sistemaresurante.repositories.UsuarioRepository;
import com.mibombay.sistemaresurante.security.CustomUserDetails;
import com.mibombay.sistemaresurante.services.InventarioExcelService;
import com.mibombay.sistemaresurante.services.InventarioFisicoService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/inventario-fisico")
public class InventarioFisicoController {

    private final InventarioFisicoService inventarioService;
    private final InventarioExcelService excelService;
    private final UsuarioRepository usuarioRepository;

    public InventarioFisicoController(InventarioFisicoService inventarioService,
                                      InventarioExcelService excelService,
                                      UsuarioRepository usuarioRepository) {
        this.inventarioService = inventarioService;
        this.excelService = excelService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String listar(Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "redirect:/login";

        List<InventarioFisico> inventarios = inventarioService.listar(empresaId);
        Map<Long, String> usuariosNombres = new HashMap<>();
        for (InventarioFisico inv : inventarios) {
            if (!usuariosNombres.containsKey(inv.getUsuarioId())) {
                String nombre = usuarioRepository.findById(inv.getUsuarioId())
                        .map(u -> u.getNombre() + (u.getApellido() != null ? " " + u.getApellido() : ""))
                        .orElse("Usuario #" + inv.getUsuarioId());
                usuariosNombres.put(inv.getUsuarioId(), nombre);
            }
        }

        model.addAttribute("inventarios", inventarios);
        model.addAttribute("usuariosNombres", usuariosNombres);
        model.addAttribute("existeConfirmadoHoy", inventarioService.existeConfirmadoHoy(empresaId));
        model.addAttribute("borradorHoy", inventarioService.obtenerBorradorHoy(empresaId));
        return "inventario-fisico/list";
    }

    @GetMapping("/nuevo")
    public String nuevo(@AuthenticationPrincipal CustomUserDetails user, RedirectAttributes redirect) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return "redirect:/login";

        if (inventarioService.existeConfirmadoHoy(empresaId)) {
            redirect.addFlashAttribute("error", "Ya se cargó el inventario físico de hoy");
            return "redirect:/inventario-fisico";
        }

        InventarioFisico borrador = inventarioService.obtenerBorradorHoy(empresaId);
        if (borrador != null) {
            return "redirect:/inventario-fisico/" + borrador.getId() + "/editar";
        }

        Map<String, BigDecimal> empty = new HashMap<>();
        InventarioFisico inv = inventarioService.crearBorrador(user.getId(), empresaId, empty, empty, empty);
        return "redirect:/inventario-fisico/" + inv.getId() + "/editar";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            InventarioFisico inv = inventarioService.obtenerPorId(id);
            if (inv.getEstado() == InventarioEstado.CONFIRMADO) {
                redirect.addFlashAttribute("error", "No se puede editar un inventario confirmado");
                return "redirect:/inventario-fisico/" + id;
            }
            List<InventarioFisicoDetalle> detalles = inventarioService.obtenerDetalles(id);
            InventarioFisicoFormDTO form = new InventarioFisicoFormDTO();
            form.setInventarioId(id);
            model.addAttribute("inventario", inv);
            model.addAttribute("detalles", detalles);
            model.addAttribute("form", form);
            return "inventario-fisico/form";
        } catch (ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/inventario-fisico";
        }
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("form") InventarioFisicoFormDTO form,
                          @AuthenticationPrincipal CustomUserDetails user,
                          RedirectAttributes redirect) {
        Long empresaId = TenantContext.getEmpresaId();
        try {
            if (form.getInventarioId() == null) {
                redirect.addFlashAttribute("error", "Inventario no especificado");
                return "redirect:/inventario-fisico";
            }
            Map<String, BigDecimal> stocksFisicos = new HashMap<>();
            Map<String, BigDecimal> mermas = new HashMap<>();
            Map<String, BigDecimal> desperdicios = new HashMap<>();
            if (form.getItems() != null) {
                for (InventarioFisicoFormDTO.ItemStock item : form.getItems()) {
                    if (item.getItemId() != null && item.getItemTipo() != null) {
                        String key = item.getItemTipo() + ":" + item.getItemId();
                        stocksFisicos.put(key,
                                item.getStockFisico() != null ? item.getStockFisico() : BigDecimal.ZERO);
                        mermas.put(key,
                                item.getMerma() != null ? item.getMerma() : BigDecimal.ZERO);
                        desperdicios.put(key,
                                item.getDesperdicio() != null ? item.getDesperdicio() : BigDecimal.ZERO);
                    }
                }
            }
            inventarioService.actualizarBorrador(form.getInventarioId(), stocksFisicos, mermas, desperdicios, empresaId);
            return "redirect:/inventario-fisico/" + form.getInventarioId() + "/revisar";
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/inventario-fisico/" + form.getInventarioId() + "/editar";
        }
    }

    @GetMapping("/{id}/revisar")
    public String revisar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            InventarioFisico inv = inventarioService.obtenerPorId(id);
            if (inv.getEstado() == InventarioEstado.CONFIRMADO) {
                return "redirect:/inventario-fisico/" + id;
            }
            List<InventarioFisicoDetalle> detalles = inventarioService.obtenerDetalles(id);
            for (InventarioFisicoDetalle d : detalles) {
                if (d.getDiferencia() != null) {
                    d.setDiferencia(d.getDiferencia().negate());
                }
            }
            int ajustes = (int) detalles.stream()
                    .filter(d -> d.getDiferencia() != null && d.getDiferencia().compareTo(BigDecimal.ZERO) != 0)
                    .count();
            model.addAttribute("inventario", inv);
            model.addAttribute("detalles", detalles);
            model.addAttribute("ajustes", ajustes);
            return "inventario-fisico/revisar";
        } catch (ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/inventario-fisico";
        }
    }

    @PostMapping("/{id}/confirmar")
    public String confirmar(@PathVariable Long id,
                            @AuthenticationPrincipal CustomUserDetails user,
                            RedirectAttributes redirect) {
        Long empresaId = TenantContext.getEmpresaId();
        try {
            InventarioFisico inv = inventarioService.confirmar(id, user.getId(), empresaId);
            int ajustes = inventarioService.contarAjustes(inv.getId());
            redirect.addFlashAttribute("success",
                    "Inventario confirmado. " + ajustes + " ajuste(s) aplicado(s) al stock.");
            return "redirect:/inventario-fisico/" + inv.getId();
        } catch (BusinessException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/inventario-fisico/" + id + "/revisar";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            InventarioFisico inv = inventarioService.obtenerPorId(id);
            List<InventarioFisicoDetalle> detalles = inventarioService.obtenerDetalles(id);
            for (InventarioFisicoDetalle d : detalles) {
                if (d.getDiferencia() != null) {
                    d.setDiferencia(d.getDiferencia().negate());
                }
            }
            int ajustes = (int) detalles.stream()
                    .filter(d -> d.getDiferencia() != null && d.getDiferencia().compareTo(BigDecimal.ZERO) != 0)
                    .count();
            String usuarioNombre = usuarioRepository.findById(inv.getUsuarioId())
                    .map(u -> u.getNombre() + (u.getApellido() != null ? " " + u.getApellido() : ""))
                    .orElse("Usuario #" + inv.getUsuarioId());
            model.addAttribute("inventario", inv);
            model.addAttribute("detalles", detalles);
            model.addAttribute("ajustes", ajustes);
            model.addAttribute("usuarioNombre", usuarioNombre);
            return "inventario-fisico/detalle";
        } catch (ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/inventario-fisico";
        }
    }

    @GetMapping("/{id}/excel")
    public void exportarExcel(@PathVariable Long id, HttpServletResponse response) throws Exception {
        InventarioFisico inv = inventarioService.obtenerPorId(id);
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = "inventario_fisico_" + inv.getFecha().toString() + ".xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        byte[] bytes = excelService.generar(inv);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }
}
