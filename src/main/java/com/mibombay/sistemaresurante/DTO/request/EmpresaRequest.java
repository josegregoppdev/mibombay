package com.mibombay.sistemaresurante.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El subdominio es obligatorio")
    private String subdominio;

    private String nombreEncargado;
    private String telefono;
    private String direccion;
    private String descripcion;
    private String email;
}
