package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.DashboardStatsDTO;
import com.mibombay.sistemaresurante.DTO.VentaNotificacionDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {

    private final SimpMessagingTemplate messagingTemplate;
    private final DashboardService dashboardService;

    public NotificacionService(SimpMessagingTemplate messagingTemplate, DashboardService dashboardService) {
        this.messagingTemplate = messagingTemplate;
        this.dashboardService = dashboardService;
    }

    public void notificarNuevaVenta(Long ventaId, java.math.BigDecimal total, String metodoPago,
                                     java.time.LocalDateTime fechaVenta, String nombreUsuario,
                                     String nombreCliente, Long empresaId) {
        DashboardStatsDTO stats = dashboardService.obtenerStatsDelDia();

        VentaNotificacionDTO notificacion = VentaNotificacionDTO.builder()
                .ventaId(ventaId)
                .total(total)
                .metodoPago(metodoPago)
                .fechaVenta(fechaVenta)
                .nombreUsuario(nombreUsuario)
                .nombreCliente(nombreCliente)
                .tipo("NUEVA_VENTA")
                .statsActualizadas(stats)
                .build();

        messagingTemplate.convertAndSend("/topic/empresa/" + empresaId + "/ventas", notificacion);
    }

    public void notificarVentaAnulada(Long ventaId, Long empresaId) {
        DashboardStatsDTO stats = dashboardService.obtenerStatsDelDia();

        VentaNotificacionDTO notificacion = VentaNotificacionDTO.builder()
                .ventaId(ventaId)
                .tipo("VENTA_ANULADA")
                .statsActualizadas(stats)
                .build();

        messagingTemplate.convertAndSend("/topic/empresa/" + empresaId + "/ventas", notificacion);
    }
}
