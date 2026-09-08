package com.mokeal.gestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class FacturacionPendienteResponse {
    private List<ServicioPendiente> servicios;
    private BigDecimal totalImporte;

    @Data
    @Builder
    public static class ServicioPendiente {
        private Long id;
        private LocalDate fecha;
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private String direccion;
        private BigDecimal importe;
    }
}