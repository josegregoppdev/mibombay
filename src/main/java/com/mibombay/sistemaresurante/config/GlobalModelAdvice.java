package com.mibombay.sistemaresurante.config;

import com.mibombay.sistemaresurante.DTO.response.EmpresaConfigResponse;
import com.mibombay.sistemaresurante.models.EstiloConfiguracion;
import com.mibombay.sistemaresurante.repositories.EmpresaRepository;
import com.mibombay.sistemaresurante.services.EstiloConfiguracionService;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

@ControllerAdvice
public class GlobalModelAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalModelAdvice.class);

    private static final Map<String, String[]> FONT_PAIRS = Map.of(
        "clasica",      new String[]{"'Instrument Serif', serif", "'Inter', sans-serif", "Instrument+Serif:ital@0;1|Inter:opsz,wght@14..32,300;14..32,500;14..32,600;14..32,700"},
        "moderna",      new String[]{"'Space Grotesk', sans-serif", "'Inter', sans-serif", "Space+Grotesk:wght@400;600;700|Inter:opsz,wght@14..32,300;14..32,500;14..32,600"},
        "elegante",     new String[]{"'Cormorant Garamond', serif", "'Lato', sans-serif", "Cormorant+Garamond:ital,wght@0,400;0,700;1,400|Lato:wght@300;400;700"},
        "condensada",   new String[]{"'Bebas Neue', sans-serif", "'Roboto Condensed', sans-serif", "Bebas+Neue&family=Roboto+Condensed:wght@300;400;700"},
        "suave",        new String[]{"'Poppins', sans-serif", "'Nunito', sans-serif", "Poppins:wght@400;600;700|Nunito:wght@300;400;600;700"},
        "bold",         new String[]{"'Lexend', sans-serif", "'DM Sans', sans-serif", "Lexend:wght@300;500;700|DM+Sans:opsz,wght@9..40,300;9..40,500;9..40,700"}
    );

    private static final Map<String, String> FONT_NAMES = Map.of(
        "clasica",    "Clásica (Instrument Serif + Inter)",
        "moderna",    "Moderna (Space Grotesk + Inter)",
        "elegante",   "Elegante (Cormorant + Lato)",
        "condensada", "Condensada (Bebas Neue + Roboto Cond)",
        "suave",      "Suave (Poppins + Nunito)",
        "bold",       "Bold (Lexend + DM Sans)"
    );

    private static final Map<String, String> TAMANO_NOMBRES = Map.of(
        "normal", "Normal",
        "grande", "Grande",
        "extra-grande", "Extra Grande"
    );

    private static final Map<String, String> TAMANO_VALORES = Map.of(
        "normal", "1rem",
        "grande", "1.125rem",
        "extra-grande", "1.25rem"
    );

    private static final Map<String, String> TEMA_NOMBRES = Map.of(
        "OSCURO", "Oscuro",
        "TIERRA", "Tierra",
        "CLARO", "Claro"
    );

    private final EmpresaRepository empresaRepository;
    private final EstiloConfiguracionService estiloConfiguracionService;

    public GlobalModelAdvice(EmpresaRepository empresaRepository,
                             EstiloConfiguracionService estiloConfiguracionService) {
        this.empresaRepository = empresaRepository;
        this.estiloConfiguracionService = estiloConfiguracionService;
    }

    @ModelAttribute("empresaConfig")
    public EmpresaConfigResponse addEmpresaConfig() {
        Long empresaId = TenantContext.getEmpresaId();
        log.debug("[GlobalModelAdvice] empresaConfig: empresaId={}", empresaId);
        if (empresaId != null) {
            return empresaRepository.findById(empresaId)
                    .map(e -> new EmpresaConfigResponse(
                            e.getNombre(),
                            e.getSubdominio()
                    ))
                    .orElse(null);
        }
        return null;
    }

    @ModelAttribute("estiloConfig")
    public EstiloConfiguracion addEstiloConfig() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            return estiloConfiguracionService.obtenerPorEmpresaId(empresaId).orElse(null);
        }
        return null;
    }

    @ModelAttribute("temaPreset")
    public TemaPreset addTemaPreset() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            String tema = estiloConfiguracionService.obtenerPorEmpresaId(empresaId)
                    .map(EstiloConfiguracion::getTema)
                    .orElse("OSCURO");
            return TemaPreset.TODOS.getOrDefault(tema, TemaPreset.TODOS.get("OSCURO"));
        }
        return TemaPreset.TODOS.get("OSCURO");
    }

    @ModelAttribute("temasDisponibles")
    public Map<String, String> addTemasDisponibles() {
        return TEMA_NOMBRES;
    }

    @ModelAttribute("currentUri")
    public String addCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("fontHeading")
    public String addFontHeading() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            return estiloConfiguracionService.obtenerPorEmpresaId(empresaId)
                    .map(e -> {
                        String[] pair = FONT_PAIRS.get(e.getFuente());
                        return pair != null ? pair[0] : FONT_PAIRS.get("clasica")[0];
                    })
                    .orElse(FONT_PAIRS.get("clasica")[0]);
        }
        return FONT_PAIRS.get("clasica")[0];
    }

    @ModelAttribute("fontBody")
    public String addFontBody() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            return estiloConfiguracionService.obtenerPorEmpresaId(empresaId)
                    .map(e -> {
                        String[] pair = FONT_PAIRS.get(e.getFuente());
                        return pair != null ? pair[1] : FONT_PAIRS.get("clasica")[1];
                    })
                    .orElse(FONT_PAIRS.get("clasica")[1]);
        }
        return FONT_PAIRS.get("clasica")[1];
    }

    @ModelAttribute("fontUrl")
    public String addFontUrl() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            return estiloConfiguracionService.obtenerPorEmpresaId(empresaId)
                    .map(e -> {
                        String[] pair = FONT_PAIRS.get(e.getFuente());
                        return pair != null ? pair[2] : FONT_PAIRS.get("clasica")[2];
                    })
                    .orElse(FONT_PAIRS.get("clasica")[2]);
        }
        return FONT_PAIRS.get("clasica")[2];
    }

    @ModelAttribute("fontOptions")
    public Map<String, String> addFontOptions() {
        return FONT_NAMES;
    }

    @ModelAttribute("tamanoOptions")
    public Map<String, String> addTamanoOptions() {
        return TAMANO_NOMBRES;
    }

    @ModelAttribute("tamanoValor")
    public String addTamanoValor() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null) {
            return estiloConfiguracionService.obtenerPorEmpresaId(empresaId)
                    .map(e -> {
                        String val = TAMANO_VALORES.get(e.getTamanoFuente());
                        return val != null ? val : "1.125rem";
                    })
                    .orElse("1.125rem");
        }
        return "1.125rem";
    }
}
