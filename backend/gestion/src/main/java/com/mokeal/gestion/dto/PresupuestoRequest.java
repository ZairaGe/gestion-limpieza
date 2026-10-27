package com.mokeal.gestion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PresupuestoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String telefono;
    private String correo;
    private String direccion;

    @NotEmpty(message = "Debe añadir al menos un concepto")
    @Size(max = 10, message = "Un presupuesto admite como máximo 10 conceptos")
    @Valid
    private List<LineaPresupuestoRequest> lineas;

    private String notas;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El descuento no puede superar el 100%")
    private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "El IVA no puede ser negativo")
    @DecimalMax(value = "100.0", message = "El IVA no puede superar el 100%")
    private BigDecimal ivaPorcentaje = BigDecimal.valueOf(21);

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDate fechaEmision;
}