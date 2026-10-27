package com.mokeal.gestion.controller;

import com.mokeal.gestion.dto.PresupuestoRequest;
import com.mokeal.gestion.dto.PresupuestoResponse;
import com.mokeal.gestion.service.PresupuestoPdfService;
import com.mokeal.gestion.service.PresupuestoService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/presupuestos")
public class PresupuestoController {

    private final PresupuestoService presupuestoService;
    private final PresupuestoPdfService presupuestoPdfService;

    public PresupuestoController(PresupuestoService presupuestoService, PresupuestoPdfService presupuestoPdfService) {
        this.presupuestoService = presupuestoService;
        this.presupuestoPdfService = presupuestoPdfService;
    }

    @GetMapping
    public List<PresupuestoResponse> listar() {
        return presupuestoService.listarTodos();
    }

    @GetMapping("/{id}")
    public PresupuestoResponse buscar(@PathVariable Long id) {
        return presupuestoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<PresupuestoResponse> crear(@Valid @RequestBody PresupuestoRequest request) {
        return ResponseEntity.status(201).body(presupuestoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PresupuestoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PresupuestoRequest request) {
        return ResponseEntity.ok(presupuestoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        presupuestoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        byte[] pdf = presupuestoPdfService.generarPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("presupuesto.pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}