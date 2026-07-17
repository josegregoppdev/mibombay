package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.CompraDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompraDetalleRepository extends JpaRepository<CompraDetalle, Long> {
    List<CompraDetalle> findByCompraId(Long compraId);
    void deleteByCompraId(Long compraId);
}
