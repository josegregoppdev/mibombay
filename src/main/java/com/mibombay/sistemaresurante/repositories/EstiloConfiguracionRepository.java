package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.EstiloConfiguracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstiloConfiguracionRepository extends JpaRepository<EstiloConfiguracion, Long> {

    Optional<EstiloConfiguracion> findByEmpresaId(Long empresaId);
}
