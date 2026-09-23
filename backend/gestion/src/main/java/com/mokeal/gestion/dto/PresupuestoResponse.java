package com.mokeal.gestion.dto;

import com.mokeal.gestion.model.EstadoPresupuesto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PresupuestoResponse {
    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private String numero;
    private List<LineaResponse> lineas;
    private BigDecimal subtotal;
    private BigDecimal descuentoPorcentaje;
    private BigDecimal total;
    private EstadoPresupuesto estado;
    private LocalDate fechaEmision;

    @Data
    @Builder
    public static class LineaResponse {
        private String concepto;
        private Double horas;
        private BigDecimal precioHora;
        private BigDecimal subtotal;
    }
}