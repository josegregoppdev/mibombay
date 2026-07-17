package com.mibombay.sistemaresurante.tenant;

import com.mibombay.sistemaresurante.repositories.EmpresaRepository;
import com.mibombay.sistemaresurante.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);

    private final EmpresaRepository empresaRepository;

    public TenantInterceptor(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/superadmin/") || requestURI.startsWith("/api/v1/empresas")) {
            TenantContext.clear();
            return true;
        }

        String header = request.getHeader("X-Empresa-Id");
        if (header != null) {
            try {
                Long id = Long.parseLong(header);
                TenantContext.setEmpresaId(id);
                log.debug("[TenantInterceptor] EmpresaId desde header: {}", id);
            } catch (NumberFormatException ignored) {}
        }

        if (TenantContext.getEmpresaId() == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof CustomUserDetails user) {
                Long empresaId = user.getEmpresaId();
                if (empresaId != null) {
                    boolean activa = empresaRepository.findById(empresaId)
                            .map(e -> e.isActivo())
                            .orElse(false);
                    if (!activa) {
                        log.warn("[TenantInterceptor] Empresa {} inactiva para usuario {}, redirigiendo a login",
                                empresaId, user.getUsername());
                        SecurityContextHolder.clearContext();
                        request.getSession().invalidate();
                        response.sendRedirect("/login?error=empresa_inactiva");
                        return false;
                    }
                }
                TenantContext.setEmpresaId(empresaId);
                log.debug("[TenantInterceptor] EmpresaId desde SecurityContext: {} (usuario: {})",
                        empresaId, user.getUsername());
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
