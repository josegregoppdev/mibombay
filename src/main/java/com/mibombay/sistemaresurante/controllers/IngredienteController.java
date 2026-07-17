package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.IngredienteDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.models.enums.UnidadMedida;
import com.mibombay.sistemaresurante.security.CustomUserDetails;
import com.mibombay.sistemaresurante.services.IngredienteService;
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

import java.util.Objects;

@Controller
public class IngredienteController {

    private final IngredienteService ingredienteService;

    public IngredienteController(IngredienteService ingredienteService) {
        this.ingredienteService = ingredienteService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ingredientes")
    public String listar(@RequestParam(required = false) String nombre,
                         @RequestParam(required = false) UnidadMedida unidad,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            Page<IngredienteDTO> ingredientesPage = ingredienteService.buscarPaginado(empresaId, nombre, unidad, PageRequest.of(page, 15));
            model.addAttribute("page", ingredientesPage);
        }
        model.addAttribute("unidades", UnidadMedida.values());
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroUnidad", unidad);
        return "ingredientes/list";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ingredientes/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("ingrediente", new IngredienteDTO());
        model.addAttribute("unidades", UnidadMedida.values());
        return "ingredientes/form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ingredientes/guardar")
    public String guardar(@Valid @ModelAttribute("ingrediente") IngredienteDTO dto,
                          BindingResult result, Model model, RedirectAttributes redirect,
                          @AuthenticationPrincipal CustomUserDetails user) {
        if (result.hasErrors()) {
            model.addAttribute("unidades", UnidadMedida.values());
            return "ingredientes/form";
        }
        try {
            dto.setEmpresaId(TenantContext.getEmpresaId());
            dto.setUsuarioId(user.getId());
            ingredienteService.crear(dto);
            redirect.addFlashAttribute("success", "Ingrediente creado correctamente");
            return "redirect:/ingredientes";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("unidades", UnidadMedida.values());
            return "ingredientes/form";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ingredientes/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        try {
            IngredienteDTO dto = ingredienteService.obtenerPorId(id);
            model.addAttribute("ingrediente", dto);
            model.addAttribute("unidades", UnidadMedida.values());
            return "ingredientes/form";
        } catch (ResourceNotFoundException e) {
            return "redirect:/ingredientes";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ingredientes/{id}/actualizar")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("ingrediente") IngredienteDTO dto,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("unidades", UnidadMedida.values());
            return "ingredientes/form";
        }
        try {
            IngredienteDTO actual = ingredienteService.obtenerPorId(id);
            if (!Objects.equals(actual.getConsumible(), dto.getConsumible())) {
                throw new BusinessException("El tipo de ingrediente (consumible/para receta) se define al crear y no se puede modificar posteriormente");
            }
            ingredienteService.actualizar(id, dto);
            redirect.addFlashAttribute("success", "Ingrediente actualizado correctamente");
            return "redirect:/ingredientes";
        } catch (ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", "Ingrediente no encontrado");
            return "redirect:/ingredientes";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("unidades", UnidadMedida.values());
            return "ingredientes/form";
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ingredientes/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            ingredienteService.eliminar(id);
            redirect.addFlashAttribute("success", "Ingrediente eliminado correctamente");
        } catch (ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", "Ingrediente no encontrado");
        }
        return "redirect:/ingredientes";
    }
}
