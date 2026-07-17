package com.mibombay.sistemaresurante.tenant;

public class TenantContext {

    private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();

    public static void setEmpresaId(Long empresaId) {
        currentTenant.set(empresaId);
    }

    public static Long getEmpresaId() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
