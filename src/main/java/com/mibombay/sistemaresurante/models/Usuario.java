package com.mibombay.sistemaresurante.models;

import com.mibombay.sistemaresurante.models.enums.Rol;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"username", "empresa_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 80)
    private String apellido;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "es_superadmin", nullable = false)
    @Builder.Default
    private boolean esSuperadmin = false;

    @PrePersist
    protected void onCreate() {
        if (empresaId == null) {
            empresaId = TenantContext.getEmpresaId();
        }
    }
}
