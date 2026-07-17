package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.DTO.DashboardStatsDTO;
import com.mibombay.sistemaresurante.DTO.request.EmpresaRequest;
import com.mibombay.sistemaresurante.DTO.response.EmpresaResponse;
import com.mibombay.sistemaresurante.DTO.response.RegistroEmpresaResponse;
import com.mibombay.sistemaresurante.security.CustomUserDetails;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import com.mibombay.sistemaresurante.services.DashboardService;
import com.mibombay.sistemaresurante.services.EmpresaService;
import com.mibombay.sistemaresurante.services.UsuarioService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    private static final Logger log = LoggerFactory.getLogger(WebController.class);

    private final EmpresaService empresaService;
    private final UsuarioService usuarioService;
    private final DashboardService dashboardService;

    public WebController(EmpresaService empresaService, UsuarioService usuarioService, DashboardService dashboardService) {
        this.empresaService = empresaService;
        this.usuarioService = usuarioService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String landing(Model model) {
        List<EmpresaResponse> empresas = empresaService.listarTodas().stream()
                .filter(EmpresaResponse::isActivo)
                .toList();
        model.addAttribute("empresas", empresas);
        return "index";
    }

    @GetMapping("/registro-empresa")
    public String formularioRegistro(Model model) {
        model.addAttribute("empresa", new EmpresaRequest());
        return "registro-empresa";
    }

    @PostMapping("/registro-empresa")
    public String procesarRegistro(@Valid @ModelAttribute("empresa") EmpresaRequest request,
            BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "registro-empresa";
        }
        try {
            RegistroEmpresaResponse registro = empresaService.crearConUsuariosPorDefecto(request);
            redirect.addFlashAttribute("registro", registro);
            return "redirect:/registro-exito";
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/registro-empresa";
        }
    }

    @GetMapping("/registro-exito")
    public String registroExito() {
        return "registro-exito";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        if (user.isEsSuperadmin()) {
            return "redirect:/superadmin/dashboard";
        }
        log.debug("[WebController] Dashboard: user={}, empresaId={}, roles={}",
                user.getUsername(), user.getEmpresaId(), user.getAuthorities());
        log.debug("[WebController] TenantContext.active={}", TenantContext.getEmpresaId());
        DashboardStatsDTO stats = dashboardService.obtenerStatsDelDia();
        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        return "dashboard";
    }

    @GetMapping("/superadmin/dashboard")
    public String superadminDashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        if (!user.isEsSuperadmin()) {
            return "redirect:/dashboard";
        }
        List<EmpresaResponse> empresas = empresaService.listarTodas();
        long totalActivas = empresas.stream().filter(EmpresaResponse::isActivo).count();
        long totalInactivas = empresas.size() - totalActivas;

        Map<Long, Long> usuariosPorEmpresa = new java.util.HashMap<>();
        for (EmpresaResponse emp : empresas) {
            usuariosPorEmpresa.put(emp.getId(), usuarioService.contarPorEmpresa(emp.getId()));
        }

        model.addAttribute("user", user);
        model.addAttribute("empresas", empresas);
        model.addAttribute("totalEmpresas", empresas.size());
        model.addAttribute("totalActivas", totalActivas);
        model.addAttribute("totalInactivas", totalInactivas);
        model.addAttribute("totalUsuarios", usuarioService.contarTotal());
        model.addAttribute("usuariosPorEmpresa", usuariosPorEmpresa);
        return "superadmin-dashboard";
    }

    @GetMapping("/api/v1/dashboard/stats")
    @ResponseBody
    public ResponseEntity<DashboardStatsDTO> obtenerStatsDashboard() {
        return ResponseEntity.ok(dashboardService.obtenerStatsDelDia());
    }
}
