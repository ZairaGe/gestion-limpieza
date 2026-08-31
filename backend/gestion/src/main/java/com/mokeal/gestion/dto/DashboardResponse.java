package com.mokeal.gestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private long serviciosHoy;
    private BigDecimal ingresosSemana;
    private long trabajadoresActivos;
    private long pendientesAsignar;
    private List<ServicioResumen> agendaHoy;
    private List<EmpleadoResumen> trabajadores;
    private List<TarifaResumen> tarifasActivas;
    private ResumenSemanal resumenSemanal;

    @Data
    @Builder
    public static class ServicioResumen {
        private Long id;
        private String clienteNombre;
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private String direccion;
        private String estado;
    }

    @Data
    @Builder
    public static class EmpleadoResumen {
        private Long id;
        private String nombre;
    }

    @Data
    @Builder
    public static class TarifaResumen {
        private String tipoServicio;
        private String zona;
        private BigDecimal precioHora;
        private BigDecimal precioFijo;
    }

    @Data
    @Builder
    public static class ResumenSemanal {
        private long totalServicios;
        private long pendientes;
    }
}