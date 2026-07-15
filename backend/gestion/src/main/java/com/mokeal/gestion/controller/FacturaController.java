package com.mokeal.gestion.controller;

import com.mokeal.gestion.dto.FacturaRequest;
import com.mokeal.gestion.dto.FacturaResponse;
import com.mokeal.gestion.service.FacturaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService facturaService;

    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @GetMapping
    public List<FacturaResponse> listar() {
        return facturaService.listarTodas();
    }

    @GetMapping("/{id}")
    public FacturaResponse buscar(@PathVariable Long id) {
        return facturaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<FacturaResponse> crear(@Valid @RequestBody FacturaRequest request) {
        FacturaResponse nueva = facturaService.crear(request);
        return ResponseEntity.status(201).body(nueva);
    }

    @PatchMapping("/{id}/pagar")
    public FacturaResponse marcarComoPagada(@PathVariable Long id) {
        return facturaService.marcarComoPagada(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        facturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}