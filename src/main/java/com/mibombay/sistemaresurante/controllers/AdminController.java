package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.request.EmpresaRequest;
import com.mibombay.sistemaresurante.DTO.response.EmpresaResponse;
import com.mibombay.sistemaresurante.services.EmpresaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/superadmin/empresas")
public class AdminController {

    private final EmpresaService empresaService;

    public AdminController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public String listarEmpresas() {
        return "redirect:/superadmin/dashboard";
    }

    @GetMapping("/nueva")
    public String formularioNuevaEmpresa(Model model) {
        model.addAttribute("empresa", new EmpresaRequest());
        return "superadmin/empresas/form";
    }

    @PostMapping("/guardar")
    public String guardarEmpresa(@Valid @ModelAttribute("empresa") EmpresaRequest request,
                                 BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "superadmin/empresas/form";
        }
        empresaService.crear(request);
        redirect.addFlashAttribute("success", "Empresa creada correctamente");
        return "redirect:/superadmin/empresas";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditarEmpresa(@PathVariable Long id, Model model) {
        EmpresaResponse empresa = empresaService.obtenerPorId(id);
        EmpresaRequest request = new EmpresaRequest();
        request.setNombre(empresa.getNombre());
        request.setSubdominio(empresa.getSubdominio());
        request.setNombreEncargado(empresa.getNombreEncargado());
        request.setTelefono(empresa.getTelefono());
        request.setDireccion(empresa.getDireccion());
        request.setDescripcion(empresa.getDescripcion());
        request.setEmail(empresa.getEmail());
        model.addAttribute("empresa", request);
        model.addAttribute("empresaId", id);
        return "superadmin/empresas/form";
    }

    @PostMapping("/{id}/actualizar")
    public String actualizarEmpresa(@PathVariable Long id, @Valid @ModelAttribute("empresa") EmpresaRequest request,
                                    BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "superadmin/empresas/form";
        }
        empresaService.actualizar(id, request);
        redirect.addFlashAttribute("success", "Empresa actualizada correctamente");
        return "redirect:/superadmin/empresas";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminarEmpresa(@PathVariable Long id, RedirectAttributes redirect) {
        empresaService.eliminar(id);
        redirect.addFlashAttribute("success", "Empresa eliminada correctamente");
        return "redirect:/superadmin/empresas";
    }

    @PostMapping("/{id}/activar")
    public String activarEmpresa(@PathVariable Long id, RedirectAttributes redirect) {
        empresaService.activar(id);
        redirect.addFlashAttribute("success", "Empresa activada correctamente");
        return "redirect:/superadmin/empresas";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivarEmpresa(@PathVariable Long id, RedirectAttributes redirect) {
        empresaService.desactivar(id);
        redirect.addFlashAttribute("success", "Empresa desactivada correctamente");
        return "redirect:/superadmin/empresas";
    }
}
