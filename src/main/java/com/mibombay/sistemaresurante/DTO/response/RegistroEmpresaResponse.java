package com.mibombay.sistemaresurante.DTO.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroEmpresaResponse {

    private Long empresaId;
    private String nombreEmpresa;
    private String subdominio;

    private String adminUsername;
    private String adminPassword;

    private String cajeroUsername;
    private String cajeroPassword;
}
