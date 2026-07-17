package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.CompraDTO;
import com.mibombay.sistemaresurante.DTO.CompraDetalleDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.models.Ingrediente;
import com.mibombay.sistemaresurante.models.Producto;
import com.mibombay.sistemaresurante.models.Proveedor;
import com.mibombay.sistemaresurante.models.enums.TipoItemCompra;
import com.mibombay.sistemaresurante.repositories.IngredienteRepository;
import com.mibombay.sistemaresurante.repositories.ProductoRepository;
import com.mibombay.sistemaresurante.repositories.ProveedorRepository;
import com.mibombay.sistemaresurante.security.CustomUserDetails;
import com.mibombay.sistemaresurante.services.CompraService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class CompraController {

    private final CompraService compraService;
    private final ProveedorRepository proveedorRepository;
    private final IngredienteRepository ingredienteRepository;
    private final ProductoRepository productoRepository;

    public CompraController(CompraService compraService,
                            ProveedorRepository proveedorRepository,
                            IngredienteRepository ingredienteRepository,
                            ProductoRepository productoRepository) {
        this.compraService = compraService;
        this.proveedorRepository = proveedorRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.productoRepository = productoRepository;
    }

    @GetMapping("/compras")
    public String listar(@RequestParam(required = false) String busqueda,
                         @RequestParam(required = false) Long proveedorId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            Page<CompraDTO> comprasPage = compraService.listar(busqueda, proveedorId, fechaDesde, fechaHasta, PageRequest.of(page, 15));
            model.addAttribute("page", comprasPage);
            model.addAttribute("proveedores", proveedorRepository.findAllByEmpresaIdAndActivoTrueOrderByRazonSocialAsc(empresaId));
        }
        model.addAttribute("filtroBusqueda", busqueda);
        model.addAttribute("filtroProveedorId", proveedorId);
        model.addAttribute("filtroFechaDesde", fechaDesde);
        model.addAttribute("filtroFechaHasta", fechaHasta);
        return "compras/list";
    }

    @GetMapping("/compras/nueva")
    public String formularioNueva(Model model) {
        CompraDTO dto = new CompraDTO();
        dto.setFechaCompra(LocalDateTime.now());
        dto.setDetalles(new ArrayList<>());
        model.addAttribute("compra", dto);
        cargarSelectores(model);
        return "compras/form";
    }

    @PostMapping("/compras/guardar")
    public String guardar(@Valid @ModelAttribute("compra") CompraDTO dto,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirect,
                          @AuthenticationPrincipal CustomUserDetails user) {
        if (result.hasErrors()) {
            cargarSelectores(model);
            return "compras/form";
        }
        try {
            dto.setEmpresaId(TenantContext.getEmpresaId());
            dto.setUsuarioId(user.getId());
            if (dto.getDetalles() == null) {
                dto.setDetalles(new ArrayList<>());
            }
            // Filtrar detalles vacios
            dto.setDetalles(dto.getDetalles().stream()
                    .filter(d -> d.getItemId() != null && d.getCantidad() != null
                            && d.getPrecioUnitario() != null && d.getCantidad().compareTo(BigDecimal.ZERO) > 0)
                    .toList());
            compraService.crear(dto);
            redirect.addFlashAttribute("success", "Compra registrada correctamente");
            return "redirect:/compras";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            cargarSelectores(model);
            return "compras/form";
        }
    }

    @PostMapping("/compras/{id}/anular")
    public String anular(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            compraService.anular(id);
            redirect.addFlashAttribute("success", "Compra anulada correctamente");
        } catch (BusinessException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/compras";
    }

    @GetMapping("/compras/{id}")
    public String ver(@PathVariable Long id, Model model) {
        try {
            CompraDTO compra = compraService.obtenerPorId(id);
            model.addAttribute("compra", compra);
            return "compras/view";
        } catch (ResourceNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/compras";
        }
    }

    @GetMapping("/compras/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        try {
            CompraDTO compra = compraService.obtenerPorId(id);
            model.addAttribute("compra", compra);
            cargarSelectores(model);
            return "compras/form";
        } catch (ResourceNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/compras";
        }
    }

    @PostMapping("/compras/{id}/actualizar")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("compra") CompraDTO dto,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirect,
                             @AuthenticationPrincipal CustomUserDetails user) {
        if (result.hasErrors()) {
            cargarSelectores(model);
            return "compras/form";
        }
        try {
            dto.setEmpresaId(TenantContext.getEmpresaId());
            dto.setUsuarioId(user.getId());
            if (dto.getDetalles() == null) {
                dto.setDetalles(new ArrayList<>());
            }
            compraService.actualizar(id, dto);
            redirect.addFlashAttribute("success", "Compra actualizada correctamente");
            return "redirect:/compras/{id}";
        } catch (BusinessException | ResourceNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            cargarSelectores(model);
            return "compras/form";
        }
    }

    private void cargarSelectores(Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            List<Proveedor> proveedores = proveedorRepository.findAllByEmpresaIdAndActivoTrueOrderByRazonSocialAsc(empresaId);
            List<Ingrediente> ingredientes = ingredienteRepository.findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(empresaId);
            List<Producto> productos = productoRepository.findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(empresaId);
            model.addAttribute("proveedores", proveedores);
            model.addAttribute("ingredientes", ingredientes);
            model.addAttribute("productos", productos);
        }
    }
}
