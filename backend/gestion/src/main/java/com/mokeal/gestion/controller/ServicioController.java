package com.mokeal.gestion.controller;

import com.mokeal.gestion.dto.ServicioRequest;
import com.mokeal.gestion.dto.ServicioResponse;
import com.mokeal.gestion.service.ServicioService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @GetMapping
    public List<ServicioResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long empleadoId) {


        if (desde != null && hasta != null) {
            return servicioService.buscarPorRangoFechas(desde, hasta);
        }
        
        if (fecha != null) {
            return servicioService.buscarPorFecha(fecha);
        }
        if (empleadoId != null) {
            return servicioService.buscarPorEmpleado(empleadoId);
        }
        return servicioService.listarTodos();
    }

    @GetMapping("/{id}")
    public ServicioResponse buscar(@PathVariable Long id) {
        return servicioService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ServicioResponse> crear(@Valid @RequestBody ServicioRequest request) {
        ServicioResponse nuevo = servicioService.crear(request);
        return ResponseEntity.status(201).body(nuevo);
    }

    @PutMapping("/{id}")
    public ServicioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ServicioRequest request) {
        return servicioService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}