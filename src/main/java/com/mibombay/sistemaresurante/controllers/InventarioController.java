package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.models.Ingrediente;
import com.mibombay.sistemaresurante.models.Producto;
import com.mibombay.sistemaresurante.models.enums.UnidadMedida;
import com.mibombay.sistemaresurante.repositories.IngredienteRepository;
import com.mibombay.sistemaresurante.repositories.ProductoRepository;
import com.mibombay.sistemaresurante.services.RecetaService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/inventario")
public class InventarioController {

    private final IngredienteRepository ingredienteRepository;
    private final ProductoRepository productoRepository;
    private final RecetaService recetaService;

    public InventarioController(IngredienteRepository ingredienteRepository,
                                ProductoRepository productoRepository,
                                RecetaService recetaService) {
        this.ingredienteRepository = ingredienteRepository;
        this.productoRepository = productoRepository;
        this.recetaService = recetaService;
    }

    @GetMapping
    public String ver(@RequestParam(required = false) String nombre,
                      @RequestParam(required = false) UnidadMedida unidad,
                      @RequestParam(required = false) Boolean tieneReceta,
                      @RequestParam(name = "pageIng", defaultValue = "0") int pageIng,
                      @RequestParam(name = "pageProd", defaultValue = "0") int pageProd,
                      Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            Specification<Ingrediente> specIng = Specification.where(
                    (root, query, cb) -> cb.equal(root.get("empresaId"), empresaId));
            specIng = specIng.and((root, query, cb) -> cb.isTrue(root.get("activo")));
            if (nombre != null && !nombre.isBlank()) {
                specIng = specIng.and((root, query, cb) ->
                        cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
            }
            if (unidad != null) {
                specIng = specIng.and((root, query, cb) ->
                        cb.equal(root.get("unidadMedida"), unidad));
            }
            model.addAttribute("pageIngredientes",
                    ingredienteRepository.findAll(specIng, PageRequest.of(pageIng, 15)));

            Specification<Producto> specProd = Specification.where(
                    (root, query, cb) -> cb.equal(root.get("empresaId"), empresaId));
            specProd = specProd.and((root, query, cb) -> cb.isTrue(root.get("activo")));
            if (nombre != null && !nombre.isBlank()) {
                specProd = specProd.and((root, query, cb) ->
                        cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
            }
            if (tieneReceta != null) {
                specProd = specProd.and((root, query, cb) ->
                        cb.equal(root.get("tieneReceta"), tieneReceta));
            }
            Page<Producto> productosPage = productoRepository.findAll(specProd, PageRequest.of(pageProd, 15));
            productosPage = productosPage.map(p -> {
                if (Boolean.TRUE.equals(p.getTieneReceta())) {
                    p.setStockActual(recetaService.calcularStock(p));
                }
                return p;
            });
            model.addAttribute("pageProductos", productosPage);
        }
        model.addAttribute("unidades", UnidadMedida.values());
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroUnidad", unidad);
        model.addAttribute("filtroTieneReceta", tieneReceta);
        return "inventario/list";
    }
}
