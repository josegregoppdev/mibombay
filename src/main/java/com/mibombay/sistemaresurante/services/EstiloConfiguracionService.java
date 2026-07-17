package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.models.EstiloConfiguracion;
import com.mibombay.sistemaresurante.repositories.EstiloConfiguracionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EstiloConfiguracionService {

    private final EstiloConfiguracionRepository repository;

    public EstiloConfiguracionService(EstiloConfiguracionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<EstiloConfiguracion> obtenerPorEmpresaId(Long empresaId) {
        return repository.findByEmpresaId(empresaId);
    }

    @Transactional
    public EstiloConfiguracion guardar(EstiloConfiguracion config) {
        return repository.save(config);
    }

    @Transactional
    public EstiloConfiguracion restablecerValores(Long empresaId) {
        EstiloConfiguracion config = repository.findByEmpresaId(empresaId)
                .orElseGet(() -> EstiloConfiguracion.builder().empresaId(empresaId).build());
        config.setTema("OSCURO");
        config.setFuente("clasica");
        config.setTamanoFuente("grande");
        return repository.save(config);
    }

    @Transactional
    public EstiloConfiguracion crearPorDefecto(Long empresaId) {
        EstiloConfiguracion config = EstiloConfiguracion.builder()
                .empresaId(empresaId)
                .build();
        return repository.save(config);
    }
}
