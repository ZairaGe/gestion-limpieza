package com.mokeal.gestion.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LineaPresupuestoRequest {

    @NotBlank(message = "El concepto es obligatorio")
    private String concepto;

    @NotNull(message = "Las horas son obligatorias")
    @Positive(message = "Las horas deben ser mayores que 0")
    private Double horas;

    @NotNull(message = "El precio por hora es obligatorio")
    @Positive(message = "El precio por hora debe ser mayor que 0")
    private BigDecimal precioHora;
}