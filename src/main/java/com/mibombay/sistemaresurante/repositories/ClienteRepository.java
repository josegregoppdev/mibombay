package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    Optional<Cliente> findByIdAndActivoTrue(Long id);

    List<Cliente> findAllByEmpresaIdAndActivoTrue(Long empresaId);

    boolean existsByNombresAndApellidosAndEmpresaIdAndActivoTrue(String nombres, String apellidos, Long empresaId);

    boolean existsByDniAndEmpresaIdAndActivoTrue(String dni, Long empresaId);

    Optional<Cliente> findByEmpresaIdAndEsConsumidorFinalTrueAndActivoTrue(Long empresaId);
}
