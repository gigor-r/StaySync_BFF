package com.staysync.bff.controller;

import com.staysync.bff.client.HabitacionesClient;
import com.staysync.bff.client.ReservasClient;
import com.staysync.bff.dto.reserva.ReservaDetalleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bff/reservas")
@RequiredArgsConstructor
@Tag(name = "BFF Reservas", description = "Gestión de reservas con datos enriquecidos de habitación")
public class ReservaBffController {

    private final ReservasClient reservasClient;
    private final HabitacionesClient habitacionesClient;

    @Operation(summary = "Listar todas las reservas")
    @GetMapping
    public ResponseEntity<Object> listar(@RequestHeader("Authorization") String authHeader) {
        return reservasClient.listar(authHeader);
    }

    @Operation(summary = "Obtener reserva detallada con información de habitación",
               description = "Agrega datos de reservas-service + habitaciones-service en una sola respuesta")
    @GetMapping("/{id}/detalle")
    public ResponseEntity<ReservaDetalleResponse> getDetalle(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        // Obtener reserva del servicio de reservas
        var reservaResp = reservasClient.getById(authHeader, id);
        @SuppressWarnings("unchecked")
        Map<String, Object> reserva = (Map<String, Object>) reservaResp.getBody();

        if (reserva == null) return ResponseEntity.notFound().build();

        // Enriquecer con datos de la habitación (puede retornar null si el servicio falla)
        Long habitacionId = reserva.get("habitacionId") instanceof Number n ? n.longValue() : null;
        ReservaDetalleResponse.HabitacionInfo habitacionInfo = null;

        if (habitacionId != null) {
            var habResp = habitacionesClient.getById(authHeader, habitacionId);
            @SuppressWarnings("unchecked")
            Map<String, Object> hab = (Map<String, Object>) habResp.getBody();
            if (hab != null) {
                habitacionInfo = ReservaDetalleResponse.HabitacionInfo.builder()
                        .id(habitacionId)
                        .numero(String.valueOf(hab.get("numero")))
                        .tipo(extraerNombreTipo(hab))
                        .precioPorNoche(new BigDecimal(String.valueOf(hab.getOrDefault("precio", "0"))))
                        .estado(String.valueOf(hab.get("estado")))
                        .build();
            }
        }

        // Calcular noches
        long noches = calcularNoches(
                String.valueOf(reserva.getOrDefault("fechaEntrada", "")),
                String.valueOf(reserva.getOrDefault("fechaSalida", "")));

        ReservaDetalleResponse detalle = ReservaDetalleResponse.builder()
                .id(toLong(reserva.get("id")))
                .codigoReserva(String.valueOf(reserva.getOrDefault("codigoReserva", "")))
                .estado(String.valueOf(reserva.getOrDefault("estado", "")))
                .fuente(String.valueOf(reserva.getOrDefault("fuente", "")))
                .fechaEntrada(String.valueOf(reserva.getOrDefault("fechaEntrada", "")))
                .fechaSalida(String.valueOf(reserva.getOrDefault("fechaSalida", "")))
                .habitacionId(habitacionId)
                .usuarioId(toLong(reserva.get("usuarioId")))
                .precioTotal(new BigDecimal(String.valueOf(reserva.getOrDefault("precioTotal", "0"))))
                .notas(String.valueOf(reserva.getOrDefault("notas", "")))
                .habitacion(habitacionInfo)
                .noches(noches)
                .build();

        return ResponseEntity.ok(detalle);
    }

    @Operation(summary = "Crear nueva reserva")
    @PostMapping
    public ResponseEntity<Object> crear(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Object body) {
        var response = reservasClient.crear(authHeader, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(response.getBody());
    }

    @Operation(summary = "Cambiar estado de reserva (checkin, checkout, etc.)")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Object> cambiarEstado(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Object body) {
        return reservasClient.cambiarEstado(authHeader, id, body);
    }

    @Operation(summary = "Cancelar reserva")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Object> cancelar(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return reservasClient.cancelar(authHeader, id);
    }

    @Operation(summary = "Reservas de un usuario específico")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<Object> porUsuario(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return reservasClient.listarPorUsuario(authHeader, usuarioId, page, size);
    }

    private long calcularNoches(String entrada, String salida) {
        try {
            return ChronoUnit.DAYS.between(LocalDate.parse(entrada), LocalDate.parse(salida));
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private String extraerNombreTipo(Map<String, Object> hab) {
        Object tipo = hab.get("tipo");
        if (tipo instanceof Map<?, ?> tipoMap) {
            return String.valueOf(((Map<String, Object>) tipoMap).getOrDefault("nombre", ""));
        }
        return String.valueOf(tipo);
    }

    private Long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        return null;
    }
}
