package com.mibombay.sistemaresurante.repositories;

import com.mibombay.sistemaresurante.models.CostoComidaDiariaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CostoComidaDiariaItemRepository extends JpaRepository<CostoComidaDiariaItem, Long> {

    List<CostoComidaDiariaItem> findByCostoComidaDiariaIdAndActivoTrue(Long costoComidaDiariaId);
}
