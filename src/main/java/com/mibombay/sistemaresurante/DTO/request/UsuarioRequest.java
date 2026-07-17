package com.mibombay.sistemaresurante.DTO.request;

import com.mibombay.sistemaresurante.models.enums.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioRequest {

    private Long empresaId;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String apellido;
    private String email;
    private String telefono;

    @NotBlank(message = "El username es obligatorio")
    private String username;

    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;
}
