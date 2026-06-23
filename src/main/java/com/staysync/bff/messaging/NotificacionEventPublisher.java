package com.staysync.bff.messaging;

import com.staysync.bff.config.RabbitMQConfig;
import com.staysync.bff.messaging.event.ReservaCreadaEvent;
import com.staysync.bff.messaging.event.ReservaEstadoCambiadoEvent;
import com.staysync.bff.messaging.event.UsuarioRegistradoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${notificaciones.enabled:true}")
    private boolean enabled;

    public void publishUsuarioRegistrado(Long usuarioId, String nombre, String email) {
        if (!enabled) return;
        try {
            var evento = new UsuarioRegistradoEvent(usuarioId, nombre, email);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_USUARIO_REGISTRO, evento);
            log.info("Evento publicado [usuario.registro]: userId={}", usuarioId);
        } catch (Exception e) {
            log.warn("No se pudo publicar evento de registro usuario={}: {}", usuarioId, e.getMessage());
        }
    }

    public void publishReservaCreada(Long reservaId, Long usuarioId, String email,
                                     String nombreUsuario, String codigo, String habitacion,
                                     String fechaEntrada, String fechaSalida, Double precioTotal) {
        if (!enabled) return;
        try {
            var evento = new ReservaCreadaEvent(
                    reservaId, usuarioId, email, nombreUsuario,
                    codigo, habitacion, fechaEntrada, fechaSalida, precioTotal);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_RESERVA_CREADA, evento);
            log.info("Evento publicado [reserva.creada]: reservaId={}", reservaId);
        } catch (Exception e) {
            log.warn("No se pudo publicar evento de reserva creada={}: {}", reservaId, e.getMessage());
        }
    }

    public void publishReservaEstadoCambiado(Long reservaId, Long usuarioId, String email,
                                             String nombreUsuario, String codigo,
                                             String habitacion, String estadoNuevo) {
        if (!enabled) return;
        try {
            var evento = new ReservaEstadoCambiadoEvent(
                    reservaId, usuarioId, email, nombreUsuario, codigo, habitacion, estadoNuevo);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_RESERVA_ESTADO, evento);
            log.info("Evento publicado [reserva.estado]: reservaId={} estado={}", reservaId, estadoNuevo);
        } catch (Exception e) {
            log.warn("No se pudo publicar evento de estado reserva={}: {}", reservaId, e.getMessage());
        }
    }
}
