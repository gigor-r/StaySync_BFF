package com.staysync.bff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staysync.bff.client.HabitacionesClient;
import com.staysync.bff.client.ReservasClient;
import com.staysync.bff.dto.dashboard.DashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bff/dashboard")
@RequiredArgsConstructor
@Tag(name = "BFF Dashboard", description = "Vista agregada para el panel de control")
public class DashboardBffController {

    private final ReservasClient reservasClient;
    private final HabitacionesClient habitacionesClient;
    private final ObjectMapper mapper;

    @Operation(summary = "Obtener datos agregados del dashboard",
               description = "Agrega información de reservas y habitaciones en una sola llamada")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestHeader("Authorization") String authHeader) {

        // Llamadas paralelas a cada servicio (independientes, con fallback propio)
        var reservasResp     = reservasClient.listar(authHeader);
        var habitacionesResp = habitacionesClient.listarTodas(authHeader);
        var disponiblesResp  = habitacionesClient.listarDisponibles(authHeader);

        List<Map<String, Object>> reservas     = parseList(reservasResp.getBody());
        List<Map<String, Object>> habitaciones = parseList(habitacionesResp.getBody());
        List<Map<String, Object>> disponibles  = parseList(disponiblesResp.getBody());

        // Calcular stats de reservas agrupando por estado
        long pendientes  = contarPorEstado(reservas, "PENDIENTE");
        long confirmadas = contarPorEstado(reservas, "CONFIRMADA");
        long enCheckin   = contarPorEstado(reservas, "CHECKIN");
        long canceladas  = contarPorEstado(reservas, "CANCELADA");

        // Stats de habitaciones
        long totalHabs  = habitaciones.size();
        long disponiblesN = disponibles.size();
        long ocupadas   = contarPorEstado(habitaciones, "OCUPADA");
        long mantenimiento = contarPorEstado(habitaciones, "MANTENIMIENTO");

        // Últimas 5 reservas recientes
        List<Map<String, Object>> recientes = reservas.stream()
                .limit(5)
                .toList();

        DashboardResponse response = DashboardResponse.builder()
                .reservas(DashboardResponse.ReservasStats.builder()
                        .total(reservas.size())
                        .pendientes(pendientes)
                        .confirmadas(confirmadas)
                        .enCheckin(enCheckin)
                        .canceladas(canceladas)
                        .build())
                .habitaciones(DashboardResponse.HabitacionesStats.builder()
                        .totalHabitaciones(totalHabs)
                        .disponibles(disponiblesN)
                        .ocupadas(ocupadas)
                        .enMantenimiento(mantenimiento)
                        .build())
                .reservasRecientes(recientes)
                .build();

        return ResponseEntity.ok(response);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseList(Object body) {
        if (body instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private long contarPorEstado(List<Map<String, Object>> items, String estado) {
        return items.stream()
                .filter(m -> estado.equals(m.get("estado")))
                .count();
    }
}
