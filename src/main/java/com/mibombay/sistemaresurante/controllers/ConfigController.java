package com.mibombay.sistemaresurante.controllers;

import com.mibombay.sistemaresurante.models.EstiloConfiguracion;
import com.mibombay.sistemaresurante.services.EstiloConfiguracionService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/config")
public class ConfigController {

    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);

    private final EstiloConfiguracionService estiloConfiguracionService;

    public ConfigController(EstiloConfiguracionService estiloConfiguracionService) {
        this.estiloConfiguracionService = estiloConfiguracionService;
    }

    @GetMapping
    public String verConfiguracion(Model model) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            return "redirect:/login";
        }
        EstiloConfiguracion config = estiloConfiguracionService
                .obtenerPorEmpresaId(empresaId)
                .orElseGet(() -> estiloConfiguracionService.crearPorDefecto(empresaId));

        model.addAttribute("estiloConfig", config);
        return "config";
    }

    @PostMapping("/guardar")
    public String guardarConfiguracion(
            @RequestParam String tema,
            @RequestParam(required = false, defaultValue = "clasica") String fuente,
            @RequestParam(name = "tamanoFuente", required = false, defaultValue = "grande") String tamanoFuente,
            RedirectAttributes redirectAttributes) {

        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            return "redirect:/login";
        }

        EstiloConfiguracion config = estiloConfiguracionService
                .obtenerPorEmpresaId(empresaId)
                .orElseGet(() -> estiloConfiguracionService.crearPorDefecto(empresaId));

        config.setTema(tema);
        config.setFuente(fuente);
        config.setTamanoFuente(tamanoFuente);

        estiloConfiguracionService.guardar(config);

        redirectAttributes.addFlashAttribute("success", "Configuración guardada exitosamente.");
        return "redirect:/config";
    }

    @PostMapping("/restablecer")
    public String restablecerValores(RedirectAttributes redirectAttributes) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            return "redirect:/login";
        }
        estiloConfiguracionService.restablecerValores(empresaId);
        redirectAttributes.addFlashAttribute("success", "Tema restablecido a Oscuro.");
        return "redirect:/config";
    }
}
