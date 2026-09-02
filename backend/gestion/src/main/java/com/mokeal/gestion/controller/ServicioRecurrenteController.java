package com.mokeal.gestion.controller;

import com.mokeal.gestion.dto.ServicioRecurrenteRequest;
import com.mokeal.gestion.service.ServicioRecurrenteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/servicios-recurrentes")
public class ServicioRecurrenteController {

    private final ServicioRecurrenteService servicioRecurrenteService;

    public ServicioRecurrenteController(ServicioRecurrenteService servicioRecurrenteService) {
        this.servicioRecurrenteService = servicioRecurrenteService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody ServicioRecurrenteRequest request) {
        int generados = servicioRecurrenteService.crearYGenerar(request);
        return ResponseEntity.status(201).body(Map.of(
                "mensaje", "Plantilla creada correctamente",
                "serviciosGenerados", generados
        ));
    }
}