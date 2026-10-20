package com.mokeal.gestion.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PresupuestoRequest {

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    @NotEmpty(message = "Debe añadir al menos una línea")
    private List<LineaPresupuestoRequest> lineas;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El descuento no puede superar el 100%")
    private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDate fechaEmision;
}