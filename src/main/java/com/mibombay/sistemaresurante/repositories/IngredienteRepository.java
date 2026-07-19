package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.Ingrediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngredienteRepository extends JpaRepository<Ingrediente, Long>, JpaSpecificationExecutor<Ingrediente> {

    List<Ingrediente> findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(Long empresaId);

    List<Ingrediente> findAllByEmpresaIdAndActivoTrueAndConsumibleFalseOrderByNombreAsc(Long empresaId);

    List<Ingrediente> findAllByEmpresaIdAndActivoTrueAndConsumibleTrueOrderByNombreAsc(Long empresaId);

    Optional<Ingrediente> findByIdAndActivoTrue(Long id);

    Optional<Ingrediente> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);

    boolean existsByNombreAndEmpresaIdAndActivoTrue(String nombre, Long empresaId);
}
