package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecetaRepository extends JpaRepository<Receta, Long> {

    Optional<Receta> findByProductoIdAndActivoTrue(Long productoId);

    boolean existsByProductoIdAndActivoTrue(Long productoId);

    List<Receta> findAllByEmpresaIdAndActivoTrue(Long empresaId);
}
