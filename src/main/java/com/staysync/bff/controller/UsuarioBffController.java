package com.staysync.bff.controller;

import com.staysync.bff.client.UsuariosClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bff/usuarios")
@RequiredArgsConstructor
@Tag(name = "BFF Usuarios", description = "Gestión de usuarios y perfiles")
public class UsuarioBffController {

    private final UsuariosClient usuariosClient;

    @Operation(summary = "Listar todos los usuarios (solo ADMIN)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> listar(
            @RequestHeader("Authorization") String authHeader) {
        return usuariosClient.listarUsuarios(authHeader);
    }

    @Operation(summary = "Obtener perfil de un usuario")
    @GetMapping("/{id}")
    public ResponseEntity<Object> getPerfil(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return usuariosClient.getPerfil(authHeader, id);
    }

    @Operation(summary = "Actualizar perfil de un usuario")
    @PutMapping("/{id}")
    public ResponseEntity<Object> updatePerfil(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Object body) {
        return usuariosClient.updatePerfil(authHeader, id, body);
    }
}
