package com.mibombay.sistemaresurante.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mibombay.sistemaresurante.DTO.VentaSuspendidaDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.models.Cliente;
import com.mibombay.sistemaresurante.models.VentaSuspendida;
import com.mibombay.sistemaresurante.repositories.ClienteRepository;
import com.mibombay.sistemaresurante.repositories.VentaSuspendidaRepository;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VentaSuspendidaService {

    private final VentaSuspendidaRepository ventaSuspendidaRepository;
    private final ClienteRepository clienteRepository;
    private final ObjectMapper objectMapper;

    public VentaSuspendidaService(VentaSuspendidaRepository ventaSuspendidaRepository,
                                  ClienteRepository clienteRepository,
                                  ObjectMapper objectMapper) {
        this.ventaSuspendidaRepository = ventaSuspendidaRepository;
        this.clienteRepository = clienteRepository;
        this.objectMapper = objectMapper;
    }

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public List<VentaSuspendidaDTO> listarPorUsuario(Long usuarioId, Long empresaId) {
        return ventaSuspendidaRepository
                .findByUsuarioIdAndEmpresaIdAndActivoTrueOrderByOrdenTabAsc(usuarioId, empresaId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public VentaSuspendidaDTO guardar(VentaSuspendidaDTO dto, Long usuarioId, Long empresaId) {
        if (dto.getItemsJson() == null || dto.getItemsJson().isBlank()) {
            dto.setItemsJson("[]");
        }
        String clienteNombre = resolverClienteNombre(dto.getClienteId(), empresaId);

        VentaSuspendida entidad;
        if (dto.getId() != null) {
            entidad = ventaSuspendidaRepository
                    .findByIdAndUsuarioIdAndEmpresaIdAndActivoTrue(dto.getId(), usuarioId, empresaId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Orden suspendida no encontrada: " + dto.getId()));
            entidad.setItemsJson(dto.getItemsJson());
            entidad.setClienteId(dto.getClienteId());
            entidad.setClienteNombre(clienteNombre);
            entidad.setMetodoPago(dto.getMetodoPago());
            entidad.setRecibidoEfectivo(dto.getRecibidoEfectivo() != null ? dto.getRecibidoEfectivo() : java.math.BigDecimal.ZERO);
            entidad.setRecibidoTransferencia(dto.getRecibidoTransferencia() != null ? dto.getRecibidoTransferencia() : java.math.BigDecimal.ZERO);
            entidad.setParaLlevar(dto.getParaLlevar() != null && dto.getParaLlevar());
            if (dto.getOrdenTab() != null) entidad.setOrdenTab(dto.getOrdenTab());
            if (dto.getEtiqueta() != null && !dto.getEtiqueta().isBlank()) entidad.setEtiqueta(dto.getEtiqueta());
        } else {
            String etiqueta = dto.getEtiqueta();
            if (etiqueta == null || etiqueta.isBlank()) {
                etiqueta = generarEtiqueta(usuarioId, empresaId);
            }
            int ordenTab = dto.getOrdenTab() != null
                    ? dto.getOrdenTab()
                    : (int) ventaSuspendidaRepository.countByUsuarioIdAndEmpresaIdAndActivoTrue(usuarioId, empresaId) + 1;
            entidad = VentaSuspendida.builder()
                    .empresaId(empresaId)
                    .usuarioId(usuarioId)
                    .etiqueta(etiqueta)
                    .itemsJson(dto.getItemsJson())
                    .clienteId(dto.getClienteId())
                    .clienteNombre(clienteNombre)
                    .metodoPago(dto.getMetodoPago())
                    .recibidoEfectivo(dto.getRecibidoEfectivo() != null ? dto.getRecibidoEfectivo() : java.math.BigDecimal.ZERO)
                    .recibidoTransferencia(dto.getRecibidoTransferencia() != null ? dto.getRecibidoTransferencia() : java.math.BigDecimal.ZERO)
                    .paraLlevar(dto.getParaLlevar() != null && dto.getParaLlevar())
                    .ordenTab(ordenTab)
                    .build();
        }
        entidad = ventaSuspendidaRepository.save(entidad);
        return toDTO(entidad);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public void eliminar(Long id, Long usuarioId, Long empresaId) {
        VentaSuspendida entidad = ventaSuspendidaRepository
                .findByIdAndUsuarioIdAndEmpresaIdAndActivoTrue(id, usuarioId, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Orden suspendida no encontrada: " + id));
        ventaSuspendidaRepository.delete(entidad);
    }

    public String generarEtiqueta(Long usuarioId, Long empresaId) {
        long count = ventaSuspendidaRepository.countByUsuarioIdAndEmpresaIdAndActivoTrue(usuarioId, empresaId);
        return "Orden " + (count + 1);
    }

    private String resolverClienteNombre(Long clienteId, Long empresaId) {
        if (clienteId == null) return null;
        return clienteRepository.findByIdAndActivoTrue(clienteId)
                .map(c -> c.getNombres() + (c.getApellidos() != null && !c.getApellidos().isBlank() ? " " + c.getApellidos() : ""))
                .orElse(null);
    }

    private VentaSuspendidaDTO toDTO(VentaSuspendida e) {
        return VentaSuspendidaDTO.builder()
                .id(e.getId())
                .empresaId(e.getEmpresaId())
                .usuarioId(e.getUsuarioId())
                .etiqueta(e.getEtiqueta())
                .itemsJson(e.getItemsJson())
                .clienteId(e.getClienteId())
                .clienteNombre(e.getClienteNombre())
                .metodoPago(e.getMetodoPago())
                .recibidoEfectivo(e.getRecibidoEfectivo())
                .recibidoTransferencia(e.getRecibidoTransferencia())
                .paraLlevar(e.getParaLlevar())
                .ordenTab(e.getOrdenTab())
                .build();
    }
}
