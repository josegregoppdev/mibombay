package com.mibombay.sistemaresurante.DTO.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaResponse {

    private Long id;
    private String nombre;
    private String subdominio;
    private String nombreEncargado;
    private String telefono;
    private String direccion;
    private String descripcion;
    private String email;
    private boolean activo;
    private LocalDateTime fechaCreacion;
}
