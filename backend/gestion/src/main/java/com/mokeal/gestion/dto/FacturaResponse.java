package com.mokeal.gestion.dto;

import com.mokeal.gestion.model.EstadoFactura;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class FacturaResponse {
    private Long id;
    private Long servicioId;
    private String numero;
    private BigDecimal importe;
    private EstadoFactura estado;
    private LocalDate fechaEmision;
}