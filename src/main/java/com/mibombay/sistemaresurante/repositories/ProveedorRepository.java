package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long>, JpaSpecificationExecutor<Proveedor> {

    Optional<Proveedor> findByIdAndActivoTrue(Long id);

    boolean existsByRazonSocialAndEmpresaIdAndActivoTrue(String razonSocial, Long empresaId);

    Optional<Proveedor> findByEmpresaIdAndEsProveedorDefectoTrueAndActivoTrue(Long empresaId);

    List<Proveedor> findAllByEmpresaIdAndActivoTrueOrderByRazonSocialAsc(Long empresaId);
}
