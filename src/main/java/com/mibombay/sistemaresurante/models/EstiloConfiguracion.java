package com.mibombay.sistemaresurante.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estilo_configuracion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstiloConfiguracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false, unique = true)
    private Long empresaId;

    @Column(length = 20, columnDefinition = "varchar(20) default 'OSCURO'")
    @Builder.Default
    private String tema = "OSCURO";

    @Column(length = 30)
    @Builder.Default
    private String fuente = "clasica";

    @Column(name = "tamano_fuente", length = 15, columnDefinition = "varchar(15) default 'grande'")
    @Builder.Default
    private String tamanoFuente = "grande";
}
