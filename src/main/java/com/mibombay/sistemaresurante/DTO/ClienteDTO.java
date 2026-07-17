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
public class ClienteDTO {

    private Long id;
    private Long empresaId;
    private Long usuarioId;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombres;

    private String apellidos;

    @NotBlank(message = "El DNI es obligatorio")
    private String dni;

    private String direccion;

    private String telefono;

    @Email(message = "Formato de correo inválido")
    private String correo;

    @Builder.Default
    private Boolean esConsumidorFinal = false;

    @Builder.Default
    private Boolean activo = true;
    private LocalDateTime fechaCreacion;
}
