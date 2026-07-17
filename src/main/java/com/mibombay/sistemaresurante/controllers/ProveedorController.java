package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.ProveedorDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.security.CustomUserDetails;
import com.mibombay.sistemaresurante.services.ProveedorService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping("/proveedores")
    public String listar(@RequestParam(required = false) String busqueda,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            Page<ProveedorDTO> proveedoresPage = proveedorService.buscarPaginado(empresaId, busqueda, PageRequest.of(page, 15));
            model.addAttribute("page", proveedoresPage);
        }
        model.addAttribute("filtroBusqueda", busqueda);
        return "proveedores/list";
    }

    @GetMapping("/proveedores/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("proveedor", new ProveedorDTO());
        return "proveedores/form";
    }

    @PostMapping("/proveedores/guardar")
    public String guardar(@Valid @ModelAttribute("proveedor") ProveedorDTO dto,
                          BindingResult result, Model model, RedirectAttributes redirect,
                          @AuthenticationPrincipal CustomUserDetails user) {
        if (result.hasErrors()) {
            return "proveedores/form";
        }
        try {
            dto.setEmpresaId(TenantContext.getEmpresaId());
            dto.setUsuarioId(user.getId());
            proveedorService.crear(dto);
            redirect.addFlashAttribute("success", "Proveedor creado correctamente");
            return "redirect:/proveedores";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "proveedores/form";
        }
    }

    @GetMapping("/proveedores/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        try {
            ProveedorDTO dto = proveedorService.obtenerPorId(id);
            model.addAttribute("proveedor", dto);
            return "proveedores/form";
        } catch (ResourceNotFoundException e) {
            return "redirect:/proveedores";
        }
    }

    @PostMapping("/proveedores/{id}/actualizar")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("proveedor") ProveedorDTO dto,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "proveedores/form";
        }
        try {
            dto.setEmpresaId(TenantContext.getEmpresaId());
            proveedorService.actualizar(id, dto);
            redirect.addFlashAttribute("success", "Proveedor actualizado correctamente");
            return "redirect:/proveedores";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "proveedores/form";
        }
    }

    @PostMapping("/proveedores/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            proveedorService.eliminar(id);
            redirect.addFlashAttribute("success", "Proveedor eliminado correctamente");
        } catch (ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", "Proveedor no encontrado");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/proveedores";
    }
}
