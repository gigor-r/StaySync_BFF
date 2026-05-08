package com.staysync.bff.controller;

import com.staysync.bff.client.UsuariosClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bff/auth")
@RequiredArgsConstructor
@Tag(name = "BFF Auth", description = "Autenticación — proxy directo a usuarios-service")
public class AuthBffController {

    private final UsuariosClient usuariosClient;

    @Operation(summary = "Login de usuario")
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Object body) {
        return usuariosClient.login(body);
    }

    @Operation(summary = "Registro de nuevo usuario")
    @PostMapping("/registro")
    public ResponseEntity<Object> registro(@RequestBody Object body) {
        return usuariosClient.registro(body);
    }

    @Operation(summary = "Renovar access token usando refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<Object> refresh(@RequestBody Object body) {
        return usuariosClient.refresh(body);
    }

    @Operation(summary = "Cerrar sesión")
    @PostMapping("/logout")
    public ResponseEntity<Object> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Object body) {
        return usuariosClient.logout(authHeader, body);
    }
}
