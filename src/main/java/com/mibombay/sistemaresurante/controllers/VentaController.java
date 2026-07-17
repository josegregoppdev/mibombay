package com.mibombay.sistemaresurante.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mibombay.sistemaresurante.DTO.RecetaDetalleDTO;
import com.mibombay.sistemaresurante.DTO.VentaDTO;
import com.mibombay.sistemaresurante.DTO.VentaSuspendidaDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.models.*;
import com.mibombay.sistemaresurante.models.enums.MetodoPago;
import com.mibombay.sistemaresurante.repositories.*;
import com.mibombay.sistemaresurante.security.CustomUserDetails;
import com.mibombay.sistemaresurante.services.VentaService;
import com.mibombay.sistemaresurante.services.VentaSuspendidaService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
import java.util.*;
import java.util.stream.Collectors;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
public class VentaController {

    private final VentaService ventaService;
    private final VentaSuspendidaService ventaSuspendidaService;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final RecetaRepository recetaRepository;
    private final RecetaDetalleRepository recetaDetalleRepository;
    private final IngredienteRepository ingredienteRepository;
    private final CierreZRepository cierreZRepository;
    private final ObjectMapper objectMapper;

    public VentaController(VentaService ventaService,
                           VentaSuspendidaService ventaSuspendidaService,
                           ProductoRepository productoRepository,
                           ClienteRepository clienteRepository,
                           RecetaRepository recetaRepository,
                           RecetaDetalleRepository recetaDetalleRepository,
                           IngredienteRepository ingredienteRepository,
                           CierreZRepository cierreZRepository,
                           ObjectMapper objectMapper) {
        this.ventaService = ventaService;
        this.ventaSuspendidaService = ventaSuspendidaService;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.recetaRepository = recetaRepository;
        this.recetaDetalleRepository = recetaDetalleRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.cierreZRepository = cierreZRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/ventas")
    public String listar(@RequestParam(required = false) String busqueda,
                         @RequestParam(required = false) Long clienteId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
                         @RequestParam(required = false) Boolean paraLlevar,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            Page<VentaDTO> ventasPage = ventaService.listarVentasConFiltros(busqueda, clienteId, fechaDesde, fechaHasta, paraLlevar, PageRequest.of(page, 15));
            model.addAttribute("page", ventasPage);
            model.addAttribute("clientes", clienteRepository.findByEmpresaIdAndEsConsumidorFinalTrueAndActivoTrue(empresaId)
                    .map(c -> java.util.List.of(c))
                    .orElseGet(java.util.Collections::emptyList));
        }
        model.addAttribute("filtroBusqueda", busqueda);
        model.addAttribute("filtroClienteId", clienteId);
        model.addAttribute("filtroFechaDesde", fechaDesde);
        model.addAttribute("filtroFechaHasta", fechaHasta);
        model.addAttribute("filtroParaLlevar", paraLlevar);
        return "ventas/list";
    }

    @GetMapping("/ventas/pos")
    public String puntoDeVenta(Model model, @AuthenticationPrincipal CustomUserDetails user) {
        Long empresaId = TenantContext.getEmpresaId();
        boolean diaCerrado = empresaId != null
                && cierreZRepository.existsByEmpresaIdAndFechaAndActivoTrue(empresaId, LocalDate.now());
        model.addAttribute("diaCerrado", diaCerrado);

        if (empresaId != null && !diaCerrado) {
            java.util.List<Producto> productos = productoRepository.findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(empresaId);
            model.addAttribute("productos", productos);
            model.addAttribute("metodosPago", MetodoPago.values());

            Cliente consumidorFinal = clienteRepository.findByEmpresaIdAndEsConsumidorFinalTrueAndActivoTrue(empresaId)
                    .orElse(null);
            model.addAttribute("consumidorFinal", consumidorFinal);

            List<Cliente> clientes = clienteRepository.findAllByEmpresaIdAndActivoTrue(empresaId);
            model.addAttribute("clientes", clientes);

            VentaDTO dto = new VentaDTO();
            dto.setClienteId(consumidorFinal != null ? consumidorFinal.getId() : null);
            model.addAttribute("venta", dto);

            // Load recipe ingredients for POS modifier modal
            Map<Long, List<Map<String, Object>>> recetasData = new HashMap<>();
            List<Receta> recetas = recetaRepository.findAllByEmpresaIdAndActivoTrue(empresaId);
            for (Receta r : recetas) {
                List<RecetaDetalle> detalles = recetaDetalleRepository.findByRecetaId(r.getId());
                List<Map<String, Object>> ingList = new ArrayList<>();
                for (RecetaDetalle d : detalles) {
                    Ingrediente ing = ingredienteRepository.findById(d.getIngredienteId()).orElse(null);
                    if (ing != null) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", ing.getId());
                        item.put("nombre", ing.getNombre());
                        item.put("unidad", ing.getUnidadMedida().name());
                        ingList.add(item);
                    }
                }
                recetasData.put(r.getProductoId(), ingList);
            }
            try {
                model.addAttribute("recetasJson", objectMapper.writeValueAsString(recetasData));
            } catch (JsonProcessingException e) {
                model.addAttribute("recetasJson", "{}");
            }

            List<VentaSuspendidaDTO> suspendidas = ventaSuspendidaService
                    .listarPorUsuario(user.getId(), empresaId);
            try {
                model.addAttribute("ventasSuspendidasJson", objectMapper.writeValueAsString(suspendidas));
            } catch (JsonProcessingException e) {
                model.addAttribute("ventasSuspendidasJson", "[]");
            }
        } else {
            model.addAttribute("recetasJson", "{}");
            model.addAttribute("ventasSuspendidasJson", "[]");
        }
        return "ventas/pos";
    }

    private void cargarSuspendidas(Model model, CustomUserDetails user) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            model.addAttribute("ventasSuspendidasJson", "[]");
            model.addAttribute("recetasJson", "{}");
            return;
        }
        List<VentaSuspendidaDTO> suspendidas = ventaSuspendidaService.listarPorUsuario(user.getId(), empresaId);
        try {
            model.addAttribute("ventasSuspendidasJson", objectMapper.writeValueAsString(suspendidas));
        } catch (JsonProcessingException e) {
            model.addAttribute("ventasSuspendidasJson", "[]");
        }
        Map<Long, List<Map<String, Object>>> recetasData = new HashMap<>();
        List<Receta> recetas = recetaRepository.findAllByEmpresaIdAndActivoTrue(empresaId);
        for (Receta r : recetas) {
            List<RecetaDetalle> detalles = recetaDetalleRepository.findByRecetaId(r.getId());
            List<Map<String, Object>> ingList = new ArrayList<>();
            for (RecetaDetalle d : detalles) {
                Ingrediente ing = ingredienteRepository.findById(d.getIngredienteId()).orElse(null);
                if (ing != null) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", ing.getId());
                    item.put("nombre", ing.getNombre());
                    item.put("unidad", ing.getUnidadMedida().name());
                    ingList.add(item);
                }
            }
            recetasData.put(r.getProductoId(), ingList);
        }
        try {
            model.addAttribute("recetasJson", objectMapper.writeValueAsString(recetasData));
        } catch (JsonProcessingException e) {
            model.addAttribute("recetasJson", "{}");
        }
    }

    @PostMapping("/ventas/guardar")
    public String guardar(@Valid @ModelAttribute("venta") VentaDTO dto,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirect,
                          @RequestParam(required = false) Long suspendidaId,
                          @AuthenticationPrincipal CustomUserDetails user) {
        Long empresaId = TenantContext.getEmpresaId();
        boolean diaCerrado = empresaId != null
                && cierreZRepository.existsByEmpresaIdAndFechaAndActivoTrue(empresaId, LocalDate.now());
        model.addAttribute("diaCerrado", diaCerrado);

        if (dto.getClienteId() == null && empresaId != null) {
            clienteRepository.findByEmpresaIdAndEsConsumidorFinalTrueAndActivoTrue(empresaId)
                    .ifPresent(cf -> dto.setClienteId(cf.getId()));
        }

        if (result.hasErrors()) {
            if (empresaId != null) {
                model.addAttribute("productos", productoRepository.findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(empresaId));
                model.addAttribute("metodosPago", MetodoPago.values());
                model.addAttribute("consumidorFinal",
                        clienteRepository.findByEmpresaIdAndEsConsumidorFinalTrueAndActivoTrue(empresaId).orElse(null));
                model.addAttribute("clientes",
                        clienteRepository.findAllByEmpresaIdAndActivoTrue(empresaId));
                cargarSuspendidas(model, user);
            }
            String errores = result.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            model.addAttribute("error", "Errores de validación: " + errores);
            return "ventas/pos";
        }
        try {
            dto.setEmpresaId(empresaId);
            dto.setUsuarioId(user.getId());
            if (dto.getDetalles() == null) {
                dto.setDetalles(new ArrayList<>());
            }
            dto.setDetalles(dto.getDetalles().stream()
                    .filter(d -> d.getProductoId() != null && d.getCantidad() != null
                            && d.getPrecioUnitario() != null && d.getCantidad().compareTo(BigDecimal.ZERO) > 0)
                    .toList());
            VentaDTO resultDto = ventaService.crear(dto);
            if (suspendidaId != null) {
                try {
                    ventaSuspendidaService.eliminar(suspendidaId, user.getId(), empresaId);
                } catch (Exception ignored) {}
            }
            redirect.addFlashAttribute("success", "Venta registrada correctamente");
            return "redirect:/ventas/" + resultDto.getId();
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            if (empresaId != null) {
                model.addAttribute("productos", productoRepository.findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(empresaId));
                model.addAttribute("metodosPago", MetodoPago.values());
                model.addAttribute("consumidorFinal",
                        clienteRepository.findByEmpresaIdAndEsConsumidorFinalTrueAndActivoTrue(empresaId).orElse(null));
                model.addAttribute("clientes",
                        clienteRepository.findAllByEmpresaIdAndActivoTrue(empresaId));
                cargarSuspendidas(model, user);
            }
            return "ventas/pos";
        }
    }

    @GetMapping("/ventas/{id}")
    public String ver(@PathVariable Long id, Model model) {
        try {
            VentaDTO venta = ventaService.obtenerVentaPorId(id);
            model.addAttribute("venta", venta);
            return "ventas/receipt";
        } catch (ResourceNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/ventas";
        }
    }

    @PostMapping("/ventas/{id}/anular")
    public String anular(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            ventaService.anular(id);
            redirect.addFlashAttribute("success", "Venta anulada correctamente");
        } catch (BusinessException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventas";
    }

    @GetMapping("/api/v1/ventas-suspendidas")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public ResponseEntity<List<VentaSuspendidaDTO>> listarSuspendidas(
            @AuthenticationPrincipal CustomUserDetails user) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) return ResponseEntity.ok(Collections.emptyList());
        return ResponseEntity.ok(ventaSuspendidaService.listarPorUsuario(user.getId(), empresaId));
    }

    @PostMapping("/api/v1/ventas-suspendidas")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public ResponseEntity<?> guardarSuspendida(@RequestBody VentaSuspendidaDTO dto,
                                                @AuthenticationPrincipal CustomUserDetails user) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empresa no definida"));
        }
        try {
            VentaSuspendidaDTO saved = ventaSuspendidaService.guardar(dto, user.getId(), empresaId);
            return ResponseEntity.ok(saved);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/v1/ventas-suspendidas/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public ResponseEntity<?> eliminarSuspendida(@PathVariable Long id,
                                                 @AuthenticationPrincipal CustomUserDetails user) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empresa no definida"));
        }
        try {
            ventaSuspendidaService.eliminar(id, user.getId(), empresaId);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
