package com.mokeal.gestion.controller;

import com.mokeal.gestion.dto.FacturacionPendienteResponse;
import com.mokeal.gestion.dto.GenerarFacturaRequest;
import com.mokeal.gestion.model.Factura;
import com.mokeal.gestion.service.FacturacionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/facturacion")
public class FacturacionController {

    private final FacturacionService facturacionService;

    public FacturacionController(FacturacionService facturacionService) {
        this.facturacionService = facturacionService;
    }

    @GetMapping("/pendientes")
    public FacturacionPendienteResponse pendientes(
            @RequestParam Long clienteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return facturacionService.obtenerPendientes(clienteId, desde, hasta);
    }

    @PostMapping("/generar")
    public ResponseEntity<Map<String, Object>> generar(@Valid @RequestBody GenerarFacturaRequest request) {
        Factura factura = facturacionService.generarFactura(request);
        return ResponseEntity.status(201).body(Map.of(
                "mensaje", "Factura generada correctamente",
                "numero", factura.getNumero(),
                "importe", factura.getImporte()
        ));
    }
}