package com.mokeal.gestion.controller;

import com.mokeal.gestion.service.PlanningService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/planning")
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @GetMapping("/empleado/{empleadoId}")
    public ResponseEntity<byte[]> descargarPlanning(
            @PathVariable Long empleadoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        byte[] pdf = planningService.generarPlanningDiario(empleadoId, fecha);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("planning_" + fecha + ".pdf")
                .build());

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}