package com.mokeal.gestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PresupuestoResponse {
    private Long id;
    private String nombre;
    private String telefono;
    private String correo;
    private String direccion;
    private String numero;
    private List<LineaResponse> lineas;
    private String notas;
    private BigDecimal subtotal;
    private BigDecimal descuentoPorcentaje;
    private BigDecimal ivaPorcentaje;
    private BigDecimal ivaImporte;
    private BigDecimal total;
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