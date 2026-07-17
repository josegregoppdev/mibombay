package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.ClienteDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.security.CustomUserDetails;
import com.mibombay.sistemaresurante.services.ClienteService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping("/clientes")
    public String listar(@RequestParam(required = false) String busqueda,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            Page<ClienteDTO> clientesPage = clienteService.buscarPaginado(empresaId, busqueda, PageRequest.of(page, 15));
            model.addAttribute("page", clientesPage);
        }
        model.addAttribute("filtroBusqueda", busqueda);
        return "clientes/list";
    }

    @GetMapping("/clientes/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("cliente", new ClienteDTO());
        return "clientes/form";
    }

    @PostMapping("/clientes/guardar")
    public String guardar(@Valid @ModelAttribute("cliente") ClienteDTO dto,
                          BindingResult result, Model model, RedirectAttributes redirect,
                          @AuthenticationPrincipal CustomUserDetails user) {
        if (result.hasErrors()) {
            return "clientes/form";
        }
        try {
            dto.setEmpresaId(TenantContext.getEmpresaId());
            dto.setUsuarioId(user.getId());
            clienteService.crear(dto);
            redirect.addFlashAttribute("success", "Cliente creado correctamente");
            return "redirect:/clientes";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "clientes/form";
        }
    }

    @GetMapping("/clientes/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        try {
            ClienteDTO dto = clienteService.obtenerPorId(id);
            model.addAttribute("cliente", dto);
            return "clientes/form";
        } catch (ResourceNotFoundException e) {
            return "redirect:/clientes";
        }
    }

    @PostMapping("/clientes/{id}/actualizar")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("cliente") ClienteDTO dto,
                             BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "clientes/form";
        }
        try {
            dto.setEmpresaId(TenantContext.getEmpresaId());
            clienteService.actualizar(id, dto);
            redirect.addFlashAttribute("success", "Cliente actualizado correctamente");
            return "redirect:/clientes";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "clientes/form";
        }
    }

    @PostMapping("/clientes/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            clienteService.eliminar(id);
            redirect.addFlashAttribute("success", "Cliente eliminado correctamente");
        } catch (ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", "Cliente no encontrado");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/clientes";
    }

    @PostMapping("/api/v1/clientes/rapido")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public ResponseEntity<Map<String, Object>> crearRapido(@RequestBody Map<String, String> body,
                                                            @AuthenticationPrincipal CustomUserDetails user) {
        String nombres = body.get("nombres");
        if (nombres == null || nombres.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio"));
        }
        try {
            ClienteDTO dto = ClienteDTO.builder()
                    .nombres(nombres.trim())
                    .apellidos(body.getOrDefault("apellidos", "").trim())
                    .dni(body.getOrDefault("dni", "").trim())
                    .telefono(body.getOrDefault("telefono", "").trim())
                    .empresaId(TenantContext.getEmpresaId())
                    .usuarioId(user.getId())
                    .build();
            ClienteDTO result = clienteService.crear(dto);
            return ResponseEntity.ok(Map.of(
                    "id", result.getId(),
                    "nombres", result.getNombres(),
                    "apellidos", result.getApellidos() != null ? result.getApellidos() : "",
                    "dni", result.getDni() != null ? result.getDni() : ""
            ));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
