package com.mokeal.gestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private BigDecimal ingresosSemana;
    private BigDecimal previstoSemana;
    private long serviciosHoy;
    private long trabajadoresActivos;
    private long sinServicioHoy;
    private List<DiaIngreso> ingresosPorDia;
    private List<ServicioHoy> agendaHoy;

    @Data
    @Builder
    public static class DiaIngreso {
        private LocalDate fecha;
        private boolean hoy;
        private BigDecimal previsto;
        private BigDecimal cobrado;
    }

    @Data
    @Builder
    public static class ServicioHoy {
        private Long id;
        private String clienteNombre;
        private String tipoServicio;
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private String estado;
        private List<String> empleados;
    }
}