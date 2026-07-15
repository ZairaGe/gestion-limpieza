package com.mokeal.gestion.controller;

import com.mokeal.gestion.model.Empleado;
import com.mokeal.gestion.service.EmpleadoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/empleados")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    public EmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @GetMapping
    public List<Empleado> listar() {
        return empleadoService.listarTodos();
    }

    @GetMapping("/{id}")
    public Empleado buscar(@PathVariable Long id) {
        return empleadoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Empleado> crear(@Valid @RequestBody Empleado empleado) {
        Empleado nuevo = empleadoService.crear(empleado);
        return ResponseEntity.status(201).body(nuevo);
    }

    @PutMapping("/{id}")
    public Empleado actualizar(@PathVariable Long id, @Valid @RequestBody Empleado empleado) {
        return empleadoService.actualizar(id, empleado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        empleadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}