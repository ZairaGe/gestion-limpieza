package com.mokeal.gestion.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class GenerarFacturaRequest {

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    @NotEmpty(message = "Debe seleccionar al menos un servicio")
    private List<Long> servicioIds;

    private String numero;

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDate fechaEmision;

    private String concepto;

    private Double descuentoPorcentaje;
}