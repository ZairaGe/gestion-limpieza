package com.mokeal.gestion.controller;

import com.mokeal.gestion.model.Tarifa;
import com.mokeal.gestion.service.TarifaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tarifas")
public class TarifaController {

    private final TarifaService tarifaService;

    public TarifaController(TarifaService tarifaService) {
        this.tarifaService = tarifaService;
    }

    @GetMapping
    public List<Tarifa> listar() {
        return tarifaService.listarTodas();
    }

    @GetMapping("/{id}")
    public Tarifa buscar(@PathVariable Long id) {
        return tarifaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Tarifa> crear(@Valid @RequestBody Tarifa tarifa) {
        Tarifa nueva = tarifaService.crear(tarifa);
        return ResponseEntity.status(201).body(nueva);
    }

    @PutMapping("/{id}")
    public Tarifa actualizar(@PathVariable Long id, @Valid @RequestBody Tarifa tarifa) {
        return tarifaService.actualizar(id, tarifa);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tarifaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}