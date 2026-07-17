package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.VentaSuspendida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VentaSuspendidaRepository extends JpaRepository<VentaSuspendida, Long> {

    List<VentaSuspendida> findByUsuarioIdAndEmpresaIdAndActivoTrueOrderByOrdenTabAsc(Long usuarioId, Long empresaId);

    Optional<VentaSuspendida> findByIdAndActivoTrue(Long id);

    long countByUsuarioIdAndEmpresaIdAndActivoTrue(Long usuarioId, Long empresaId);

    Optional<VentaSuspendida> findByIdAndUsuarioIdAndEmpresaIdAndActivoTrue(Long id, Long usuarioId, Long empresaId);
}
