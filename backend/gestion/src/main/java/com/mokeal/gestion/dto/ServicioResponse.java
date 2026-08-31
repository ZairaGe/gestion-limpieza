package com.mokeal.gestion.dto;

import com.mokeal.gestion.model.EstadoServicio;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
@Builder
public class ServicioResponse {
    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private Long tarifaId;
    private String tarifaTipoServicio;
    private String tarifaZona;
    private String direccion;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private EstadoServicio estado;
    private Set<EmpleadoResumen> empleados;

    @Data
    @Builder
    public static class EmpleadoResumen {
        private Long id;
        private String nombre;
    }
}