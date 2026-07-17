package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findBySubdominio(String subdominio);

    boolean existsBySubdominio(String subdominio);

    List<Empresa> findAllByActivoTrue();

    Optional<Empresa> findByIdAndActivoTrue(Long id);
}
