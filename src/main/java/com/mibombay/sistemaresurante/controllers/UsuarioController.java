package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.request.UsuarioRequest;
import com.mibombay.sistemaresurante.DTO.response.UsuarioResponse;
import com.mibombay.sistemaresurante.models.enums.Rol;
import com.mibombay.sistemaresurante.security.CustomUserDetails;
import com.mibombay.sistemaresurante.services.UsuarioService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    private Rol[] getRolesDisponibles(CustomUserDetails user) {
        if (user.isEsSuperadmin()) {
            return Rol.values();
        }
        return new Rol[]{Rol.CAJERO, Rol.ADMIN};
    }

    @GetMapping("/usuarios")
    public String listarUsuarios(Model model, @AuthenticationPrincipal CustomUserDetails user) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            List<UsuarioResponse> usuarios = usuarioService.listarUsuariosPorEmpresa(empresaId);
            model.addAttribute("usuarios", usuarios);
        }
        return "usuarios/list";
    }

    @GetMapping("/usuarios/nuevo")
    public String formularioNuevoUsuario(Model model, @AuthenticationPrincipal CustomUserDetails user) {
        model.addAttribute("usuario", new UsuarioRequest());
        model.addAttribute("roles", getRolesDisponibles(user));
        return "usuarios/form";
    }

    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(@Valid @ModelAttribute("usuario") UsuarioRequest request,
                                 BindingResult result, Model model, RedirectAttributes redirect,
                                 @AuthenticationPrincipal CustomUserDetails user) {
        if (result.hasErrors()) {
            model.addAttribute("roles", getRolesDisponibles(user));
            return "usuarios/form";
        }
        request.setEmpresaId(TenantContext.getEmpresaId());
        usuarioService.crearUsuario(request);
        redirect.addFlashAttribute("success", "Usuario creado correctamente");
        return "redirect:/usuarios";
    }

    @GetMapping("/usuarios/{id}/editar")
    public String formularioEditarUsuario(@PathVariable Long id, Model model,
                                          @AuthenticationPrincipal CustomUserDetails user) {
        UsuarioResponse usuario = usuarioService.obtenerUsuarioPorId(id);
        UsuarioRequest request = new UsuarioRequest();
        request.setNombre(usuario.getNombre());
        request.setApellido(usuario.getApellido());
        request.setEmail(usuario.getEmail());
        request.setTelefono(usuario.getTelefono());
        request.setUsername(usuario.getUsername());
        request.setRol(usuario.getRol());
        request.setEmpresaId(usuario.getEmpresaId());
        model.addAttribute("usuario", request);
        model.addAttribute("usuarioId", id);
        model.addAttribute("roles", getRolesDisponibles(user));
        return "usuarios/form";
    }

    @PostMapping("/usuarios/{id}/actualizar")
    public String actualizarUsuario(@PathVariable Long id, @Valid @ModelAttribute("usuario") UsuarioRequest request,
                                    BindingResult result, Model model, RedirectAttributes redirect,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        if (result.hasErrors()) {
            model.addAttribute("roles", getRolesDisponibles(user));
            return "usuarios/form";
        }
        request.setEmpresaId(TenantContext.getEmpresaId());
        usuarioService.actualizarUsuario(id, request);
        redirect.addFlashAttribute("success", "Usuario actualizado correctamente");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}/eliminar")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirect) {
        usuarioService.eliminarUsuario(id);
        redirect.addFlashAttribute("success", "Usuario eliminado correctamente");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}/activar")
    public String activarUsuario(@PathVariable Long id, RedirectAttributes redirect) {
        usuarioService.activarUsuario(id);
        redirect.addFlashAttribute("success", "Usuario activado correctamente");
        return "redirect:/usuarios";
    }
}
