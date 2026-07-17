package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.RecetaDTO;
import com.mibombay.sistemaresurante.DTO.RecetaDetalleDTO;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.mapper.RecetaDetalleMapper;
import com.mibombay.sistemaresurante.mapper.RecetaMapper;
import com.mibombay.sistemaresurante.models.Ingrediente;
import com.mibombay.sistemaresurante.models.Producto;
import com.mibombay.sistemaresurante.models.Receta;
import com.mibombay.sistemaresurante.models.RecetaDetalle;
import com.mibombay.sistemaresurante.repositories.IngredienteRepository;
import com.mibombay.sistemaresurante.repositories.ProductoRepository;
import com.mibombay.sistemaresurante.repositories.RecetaDetalleRepository;
import com.mibombay.sistemaresurante.repositories.RecetaRepository;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RecetaService {

    private final RecetaRepository recetaRepository;
    private final RecetaDetalleRepository detalleRepository;
    private final ProductoRepository productoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final RecetaMapper recetaMapper;
    private final RecetaDetalleMapper detalleMapper;

    public RecetaService(RecetaRepository recetaRepository,
                         RecetaDetalleRepository detalleRepository,
                         ProductoRepository productoRepository,
                         IngredienteRepository ingredienteRepository,
                         RecetaMapper recetaMapper,
                         RecetaDetalleMapper detalleMapper) {
        this.recetaRepository = recetaRepository;
        this.detalleRepository = detalleRepository;
        this.productoRepository = productoRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.recetaMapper = recetaMapper;
        this.detalleMapper = detalleMapper;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public RecetaDTO obtenerPorProducto(Long productoId) {
        verificarProductoPerteneceAEmpresa(productoId);
        Receta receta = recetaRepository.findByProductoIdAndActivoTrue(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("La receta no encontrada para el producto: " + productoId));
        return construirRecetaDTO(receta);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean existeReceta(Long productoId) {
        return recetaRepository.existsByProductoIdAndActivoTrue(productoId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public RecetaDTO guardar(Long productoId, String nombreReceta, List<RecetaDetalleDTO> detallesDTO) {
        Producto producto = productoRepository.findByIdAndActivoTrue(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        verificarPerteneceAEmpresa(producto);

        Long empresaId = TenantContext.getEmpresaId();

        Receta receta = recetaRepository.findByProductoIdAndActivoTrue(productoId)
                .orElse(Receta.builder()
                        .productoId(productoId)
                        .empresaId(empresaId)
                        .build());

        receta.setNombreReceta(nombreReceta);
        receta.setEmpresaId(empresaId);

        if (receta.getId() == null) {
            receta = recetaRepository.save(receta);
        }

        receta.getDetalles().clear();

        BigDecimal costoTotal = BigDecimal.ZERO;

        for (RecetaDetalleDTO detDTO : detallesDTO) {
            if (detDTO.getIngredienteId() == null || detDTO.getCantidad() == null) continue;

            Ingrediente ingrediente = ingredienteRepository.findByIdAndActivoTrue(detDTO.getIngredienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado: " + detDTO.getIngredienteId()));

            BigDecimal precioCompra = ingrediente.getPrecioCompra() != null ? ingrediente.getPrecioCompra() : BigDecimal.ZERO;
            BigDecimal costo = detDTO.getCantidad().multiply(precioCompra).setScale(2, RoundingMode.HALF_UP);

            RecetaDetalle detalle = RecetaDetalle.builder()
                    .recetaId(receta.getId())
                    .ingredienteId(detDTO.getIngredienteId())
                    .cantidad(detDTO.getCantidad())
                    .costo(costo)
                    .build();

            receta.getDetalles().add(detalle);
            costoTotal = costoTotal.add(costo);
        }

        receta.setCostoReceta(costoTotal.setScale(2, RoundingMode.HALF_UP));
        receta = recetaRepository.save(receta);

        return construirRecetaDTO(receta);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void eliminar(Long productoId) {
        Receta receta = recetaRepository.findByProductoIdAndActivoTrue(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada para el producto: " + productoId));
        verificarPerteneceAEmpresa(productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId)));

        detalleRepository.findByRecetaId(receta.getId())
                .forEach(d -> detalleRepository.delete(d));

        receta.setActivo(false);
        recetaRepository.save(receta);
    }

    public BigDecimal calcularStock(Producto producto) {
        if (!Boolean.TRUE.equals(producto.getTieneReceta())) return null;

        Optional<Receta> optReceta = recetaRepository.findByProductoIdAndActivoTrue(producto.getId());
        if (optReceta.isEmpty()) return null;

        List<RecetaDetalle> detalles = detalleRepository.findByRecetaId(optReceta.get().getId());
        if (detalles.isEmpty()) return BigDecimal.ZERO;

        BigDecimal stockMinimo = null;
        for (RecetaDetalle det : detalles) {
            Ingrediente ing = det.getIngrediente();
            if (ing == null) continue;

            BigDecimal stockIng = ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO;
            if (det.getCantidad().compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal stockCalculado = stockIng.divide(det.getCantidad(), 2, RoundingMode.FLOOR);
            if (stockMinimo == null || stockCalculado.compareTo(stockMinimo) < 0) {
                stockMinimo = stockCalculado;
            }
        }

        return stockMinimo;
    }

    private RecetaDTO construirRecetaDTO(Receta receta) {
        RecetaDTO dto = recetaMapper.toDTO(receta);

        Producto producto = productoRepository.findById(receta.getProductoId()).orElse(null);
        if (producto != null) {
            dto.setNombreProducto(producto.getNombre());
            dto.setTieneReceta(producto.getTieneReceta());
        }

        List<RecetaDetalle> detalles = detalleRepository.findByRecetaId(receta.getId());
        List<RecetaDetalleDTO> detallesDTO = detalles.stream().map(d -> {
            RecetaDetalleDTO detDTO = detalleMapper.toDTO(d);
            if (d.getIngrediente() != null) {
                detDTO.setNombreIngrediente(d.getIngrediente().getNombre());
                detDTO.setUnidad(d.getIngrediente().getUnidadMedida().name());
            }
            return detDTO;
        }).toList();
        dto.setDetalles(detallesDTO);

        return dto;
    }

    private void verificarPerteneceAEmpresa(Producto producto) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null && !empresaId.equals(producto.getEmpresaId())) {
            throw new ResourceNotFoundException("Producto no encontrado: " + producto.getId());
        }
    }

    private void verificarProductoPerteneceAEmpresa(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
        verificarPerteneceAEmpresa(producto);
    }
}
