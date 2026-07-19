package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.ProductoDTO;
import com.mibombay.sistemaresurante.DTO.RecetaDTO;
import com.mibombay.sistemaresurante.DTO.RecetaDetalleDTO;
import com.mibombay.sistemaresurante.models.enums.UnidadMedida;
import com.mibombay.sistemaresurante.services.IngredienteService;
import com.mibombay.sistemaresurante.services.ProductoService;
import com.mibombay.sistemaresurante.services.RecetaService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/productos/{productoId}/receta")
public class RecetaController {

    private final RecetaService recetaService;
    private final ProductoService productoService;
    private final IngredienteService ingredienteService;

    public RecetaController(RecetaService recetaService,
                            ProductoService productoService,
                            IngredienteService ingredienteService) {
        this.recetaService = recetaService;
        this.productoService = productoService;
        this.ingredienteService = ingredienteService;
    }

    @GetMapping
    public String verReceta(@PathVariable Long productoId, Model model) {
        ProductoDTO producto = productoService.obtenerPorId(productoId);
        model.addAttribute("producto", producto);

        if (recetaService.existeReceta(productoId)) {
            RecetaDTO receta = recetaService.obtenerPorProducto(productoId);
            model.addAttribute("receta", receta);
        }

        return "recetas/view";
    }

    @GetMapping("/editar")
    public String formularioEditar(@PathVariable Long productoId, Model model) {
        ProductoDTO producto = productoService.obtenerPorId(productoId);
        model.addAttribute("producto", producto);

        RecetaDTO receta;
        if (recetaService.existeReceta(productoId)) {
            receta = recetaService.obtenerPorProducto(productoId);
        } else {
            receta = RecetaDTO.builder()
                    .productoId(productoId)
                    .nombreProducto(producto.getNombre())
                    .detalles(List.of())
                    .build();
        }
        model.addAttribute("receta", receta);

        Long empresaId = TenantContext.getEmpresaId();
        Page<com.mibombay.sistemaresurante.DTO.IngredienteDTO> ingredientes =
                ingredienteService.listarIngredientesConFiltros(empresaId, null, null, PageRequest.of(0, 500));
        model.addAttribute("ingredientes", ingredientes.getContent());
        model.addAttribute("unidades", UnidadMedida.values());

        return "recetas/form";
    }

    @PostMapping("/guardar")
    public String guardarReceta(@PathVariable Long productoId,
                                @RequestParam(required = false) String nombreReceta,
                                @RequestParam(required = false) List<Long> ingredienteIds,
                                @RequestParam(required = false) List<java.math.BigDecimal> cantidades,
                                RedirectAttributes redirect) {
        try {
            List<RecetaDetalleDTO> detalles = new java.util.ArrayList<>();
            if (ingredienteIds != null && cantidades != null) {
                for (int i = 0; i < ingredienteIds.size(); i++) {
                    if (ingredienteIds.get(i) != null && cantidades.get(i) != null
                            && cantidades.get(i).compareTo(java.math.BigDecimal.ZERO) > 0) {
                        detalles.add(RecetaDetalleDTO.builder()
                                .ingredienteId(ingredienteIds.get(i))
                                .cantidad(cantidades.get(i))
                                .build());
                    }
                }
            }
            recetaService.guardar(productoId, nombreReceta, detalles);
            redirect.addFlashAttribute("success", "Receta guardada correctamente");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al guardar la receta: " + e.getMessage());
        }
        return "redirect:/productos/" + productoId + "/receta";
    }

    @PostMapping("/eliminar")
    public String eliminarReceta(@PathVariable Long productoId, RedirectAttributes redirect) {
        try {
            recetaService.eliminar(productoId);
            redirect.addFlashAttribute("success", "Receta eliminada correctamente");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al eliminar la receta");
        }
        return "redirect:/productos/" + productoId + "/receta";
    }
}
