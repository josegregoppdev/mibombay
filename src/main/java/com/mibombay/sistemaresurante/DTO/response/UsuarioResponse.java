package com.mibombay.sistemaresurante.DTO.response;

import com.mibombay.sistemaresurante.models.enums.Rol;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponse {

    private Long id;
    private Long empresaId;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String username;
    private Rol rol;
    private boolean activo;
}
