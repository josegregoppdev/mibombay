package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.ProductoDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.security.CustomUserDetails;
import com.mibombay.sistemaresurante.services.ProductoService;
import com.mibombay.sistemaresurante.services.RecetaService;
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
public class ProductoController {

    private final ProductoService productoService;
    private final RecetaService recetaService;

    public ProductoController(ProductoService productoService, RecetaService recetaService) {
        this.productoService = productoService;
        this.recetaService = recetaService;
    }

    @GetMapping("/productos")
    public String listar(@RequestParam(required = false) String nombre,
                         @RequestParam(required = false) Boolean tieneReceta,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            Page<ProductoDTO> productosPage = productoService.buscarPaginado(empresaId, nombre, tieneReceta, PageRequest.of(page, 15));
            model.addAttribute("page", productosPage);
        }
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroTieneReceta", tieneReceta);
        return "productos/list";
    }

    @GetMapping("/productos/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("producto", new ProductoDTO());
        return "productos/form";
    }

    @PostMapping("/productos/guardar")
    public String guardar(@Valid @ModelAttribute("producto") ProductoDTO dto,
                          BindingResult result, Model model, RedirectAttributes redirect,
                          @AuthenticationPrincipal CustomUserDetails user) {
        if (result.hasErrors()) {
            return "productos/form";
        }
        try {
            dto.setEmpresaId(TenantContext.getEmpresaId());
            dto.setUsuarioId(user.getId());
            ProductoDTO creado = productoService.crear(dto);
            if (Boolean.TRUE.equals(dto.getTieneReceta())) {
                redirect.addFlashAttribute("success", "Producto creado. Ahora configura la receta");
                return "redirect:/productos/" + creado.getId() + "/receta/editar";
            }
            redirect.addFlashAttribute("success", "Producto creado correctamente");
            return "redirect:/productos";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "productos/form";
        }
    }

    @GetMapping("/productos/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        try {
            ProductoDTO dto = productoService.obtenerPorId(id);
            model.addAttribute("producto", dto);
            return "productos/form";
        } catch (ResourceNotFoundException e) {
            return "redirect:/productos";
        }
    }

    @PostMapping("/productos/{id}/actualizar")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("producto") ProductoDTO dto,
                             BindingResult result, Model model, RedirectAttributes redirect,
                             @AuthenticationPrincipal CustomUserDetails user) {
        if (result.hasErrors()) {
            return "productos/form";
        }
        try {
            dto.setEmpresaId(TenantContext.getEmpresaId());
            dto.setUsuarioId(user.getId());
            productoService.actualizar(id, dto);
            redirect.addFlashAttribute("success", "Producto actualizado correctamente");
            return "redirect:/productos";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "productos/form";
        }
    }

    @PostMapping("/productos/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            productoService.eliminar(id);
            redirect.addFlashAttribute("success", "Producto eliminado correctamente");
        } catch (ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", "Producto no encontrado");
        }
        return "redirect:/productos";
    }
}
