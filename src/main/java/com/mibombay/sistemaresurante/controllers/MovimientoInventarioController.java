package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.MovimientoInventarioDTO;
import com.mibombay.sistemaresurante.models.Ingrediente;
import com.mibombay.sistemaresurante.models.Producto;
import com.mibombay.sistemaresurante.models.enums.MovimientoTipo;
import com.mibombay.sistemaresurante.repositories.IngredienteRepository;
import com.mibombay.sistemaresurante.repositories.ProductoRepository;
import com.mibombay.sistemaresurante.services.MovimientoInventarioService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/movimientos")
public class MovimientoInventarioController {

    private final MovimientoInventarioService service;
    private final IngredienteRepository ingredienteRepository;
    private final ProductoRepository productoRepository;

    public MovimientoInventarioController(MovimientoInventarioService service,
                                          IngredienteRepository ingredienteRepository,
                                          ProductoRepository productoRepository) {
        this.service = service;
        this.ingredienteRepository = ingredienteRepository;
        this.productoRepository = productoRepository;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String itemTipo,
                         @RequestParam(required = false) Long itemId,
                         @RequestParam(required = false) MovimientoTipo movimientoTipo,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            Page<MovimientoInventarioDTO> movimientos = service.listar(itemTipo, itemId, movimientoTipo, PageRequest.of(page, 20));
            model.addAttribute("page", movimientos);
            model.addAttribute("ingredientes", ingredienteRepository.findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(empresaId));
            model.addAttribute("productos", productoRepository.findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(empresaId));
        }
        model.addAttribute("movimientoTipos", MovimientoTipo.values());
        model.addAttribute("filtroItemTipo", itemTipo);
        model.addAttribute("filtroItemId", itemId);
        model.addAttribute("filtroMovimientoTipo", movimientoTipo);
        return "movimientos/list";
    }
}
