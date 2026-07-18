package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.MovimientoInventarioDTO;
import com.mibombay.sistemaresurante.mapper.MovimientoInventarioMapper;
import com.mibombay.sistemaresurante.models.MovimientoInventario;
import com.mibombay.sistemaresurante.models.enums.MovimientoTipo;
import com.mibombay.sistemaresurante.repositories.MovimientoInventarioRepository;
import com.mibombay.sistemaresurante.repositories.UsuarioRepository;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository repository;
    private final MovimientoInventarioMapper mapper;
    private final UsuarioRepository usuarioRepository;

    public MovimientoInventarioService(MovimientoInventarioRepository repository,
                                       MovimientoInventarioMapper mapper,
                                       UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.usuarioRepository = usuarioRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<MovimientoInventarioDTO> listar(String itemTipo, Long itemId,
                                                 MovimientoTipo movimientoTipo,
                                                 Pageable pageable) {
        Long empresaId = TenantContext.getEmpresaId();
        Specification<MovimientoInventario> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("empresaId"), empresaId));
            if (itemTipo != null && !itemTipo.isBlank()) {
                predicates.add(cb.equal(root.get("itemTipo"), itemTipo));
            }
            if (itemId != null) {
                predicates.add(cb.equal(root.get("itemId"), itemId));
            }
            if (movimientoTipo != null) {
                predicates.add(cb.equal(root.get("movimientoTipo"), movimientoTipo));
            }
            query.orderBy(cb.desc(root.get("fechaMovimiento")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable).map(this::toDTOConUsuario);
    }

    private MovimientoInventarioDTO toDTOConUsuario(MovimientoInventario mov) {
        MovimientoInventarioDTO dto = mapper.toDTO(mov);
        dto.setNombreUsuario(usuarioRepository.findById(mov.getUsuarioId())
                .map(u -> u.getNombre() + (u.getApellido() != null ? " " + u.getApellido() : ""))
                .orElse("Usuario #" + mov.getUsuarioId()));
        return dto;
    }
}
