package com.mibombay.sistemaresurante.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorDTO {

    private Long id;
    private Long empresaId;
    private Long usuarioId;

    @NotBlank(message = "La razón social es obligatoria")
    private String razonSocial;

    private String contacto;

    private String telefono;

    @Email(message = "Formato de correo inválido")
    private String correo;

    private String direccion;

    @Builder.Default
    private Boolean esProveedorDefecto = false;

    @Builder.Default
    private Boolean activo = true;
    private LocalDateTime fechaCreacion;
}
