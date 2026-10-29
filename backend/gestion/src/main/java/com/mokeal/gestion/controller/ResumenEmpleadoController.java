package com.mokeal.gestion.controller;

import com.mokeal.gestion.service.ResumenEmpleadoPdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/empleados")
public class ResumenEmpleadoController {

    private final ResumenEmpleadoPdfService resumenService;

    public ResumenEmpleadoController(ResumenEmpleadoPdfService resumenService) {
        this.resumenService = resumenService;
    }

    @GetMapping("/{id}/resumen-mensual")
    public ResponseEntity<byte[]> resumenMensual(@PathVariable Long id, @RequestParam String mes) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }

        byte[] pdf = resumenService.generar(id, ym);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("resumen_empleado_" + mes + ".pdf").build());

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}