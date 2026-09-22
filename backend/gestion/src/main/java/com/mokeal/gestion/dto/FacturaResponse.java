package com.mokeal.gestion.dto;

import com.mokeal.gestion.model.EstadoFactura;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class FacturaResponse {
    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private String numero;
    private BigDecimal importe;
    private EstadoFactura estado;
    private LocalDate fechaEmision;
    private int cantidadServicios;
    private String concepto;
    private BigDecimal subtotal;
    private BigDecimal descuentoPorcentaje;
    private BigDecimal ivaImporte;
}