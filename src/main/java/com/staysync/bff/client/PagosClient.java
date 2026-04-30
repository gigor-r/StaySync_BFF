package com.staysync.bff.client;

import com.staysync.bff.exception.DownstreamServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class PagosClient {

    private final RestTemplate restTemplate;

    @Value("${services.pagos.url}")
    private String baseUrl;

    @CircuitBreaker(name = "pagosCB", fallbackMethod = "mutarFallback")
    public ResponseEntity<Object> procesarPago(String authHeader, Object body) {
        return restTemplate.exchange(
                baseUrl + "/api/v1/pagos",
                HttpMethod.POST,
                new HttpEntity<>(body, UsuariosClient.buildHeaders(authHeader)),
                Object.class);
    }

    @CircuitBreaker(name = "pagosCB", fallbackMethod = "getOneFallback")
    public ResponseEntity<Object> getPagoById(String authHeader, Long id) {
        return restTemplate.exchange(
                baseUrl + "/api/v1/pagos/" + id,
                HttpMethod.GET,
                new HttpEntity<>(UsuariosClient.buildHeaders(authHeader)),
                Object.class);
    }

    @CircuitBreaker(name = "pagosCB", fallbackMethod = "listarFallback")
    public ResponseEntity<Object> getPagosPorReserva(String authHeader, Long reservaId) {
        return restTemplate.exchange(
                baseUrl + "/api/v1/pagos/reserva/" + reservaId,
                HttpMethod.GET,
                new HttpEntity<>(UsuariosClient.buildHeaders(authHeader)),
                Object.class);
    }

    @CircuitBreaker(name = "pagosCB", fallbackMethod = "mutarFallback")
    public ResponseEntity<Object> solicitarReembolso(String authHeader, Long pagoId, Object body) {
        return restTemplate.exchange(
                baseUrl + "/api/v1/pagos/" + pagoId + "/reembolso",
                HttpMethod.POST,
                new HttpEntity<>(body, UsuariosClient.buildHeaders(authHeader)),
                Object.class);
    }

    public ResponseEntity<Object> mutarFallback(String authHeader, Object body, Exception ex) {
        log.error("pagos-service no disponible: {}", ex.getMessage());
        throw new DownstreamServiceException("El servicio de pagos no está disponible. Intente nuevamente.");
    }

    public ResponseEntity<Object> mutarFallback(String authHeader, Long id, Object body, Exception ex) {
        log.error("pagos-service no disponible: {}", ex.getMessage());
        throw new DownstreamServiceException("El servicio de pagos no está disponible.");
    }

    public ResponseEntity<Object> getOneFallback(String authHeader, Long id, Exception ex) {
        log.warn("pagos-service no disponible para id={}: {}", id, ex.getMessage());
        throw new DownstreamServiceException("El servicio de pagos no está disponible.");
    }

    public ResponseEntity<Object> listarFallback(String authHeader, Long reservaId, Exception ex) {
        log.warn("pagos-service no disponible: {}", ex.getMessage());
        return ResponseEntity.ok(Collections.emptyList());
    }
}
