package com.mokeal.gestion.controller;

import com.mokeal.gestion.service.HorarioPdfService;
import com.mokeal.gestion.service.PlanningService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/planning")
public class PlanningController {

    private final PlanningService planningService;
    private final HorarioPdfService horarioPdfService;

    public PlanningController(PlanningService planningService, HorarioPdfService horarioPdfService) {
        this.planningService = planningService;
        this.horarioPdfService = horarioPdfService;
    }

    @GetMapping("/empleado/{empleadoId}")
    public ResponseEntity<byte[]> descargarPlanning(
            @PathVariable Long empleadoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return pdf(planningService.generarPlanningDiario(empleadoId, fecha), "planning_" + fecha + ".pdf");
    }

    @GetMapping("/general")
    public ResponseEntity<byte[]> descargarGeneral(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (hasta.isBefore(desde))
            return ResponseEntity.badRequest().build();
        return pdf(horarioPdfService.generarGeneral(desde, hasta), "horario_" + desde + "_" + hasta + ".pdf");
    }

    @GetMapping("/por-trabajador")
    public ResponseEntity<byte[]> descargarPorTrabajador(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (hasta.isBefore(desde))
            return ResponseEntity.badRequest().build();
        return pdf(horarioPdfService.generarPorTrabajador(desde, hasta),
                "planning_trabajadores_" + desde + "_" + hasta + ".pdf");
    }

    private ResponseEntity<byte[]> pdf(byte[] contenido, String nombre) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(nombre).build());
        return ResponseEntity.ok().headers(headers).body(contenido);
    }
}