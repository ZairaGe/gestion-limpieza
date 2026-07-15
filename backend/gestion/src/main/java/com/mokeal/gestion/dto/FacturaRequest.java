package com.mokeal.gestion.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FacturaRequest {

    @NotNull(message = "El servicio es obligatorio")
    private Long servicioId;

    @NotBlank(message = "El número de factura es obligatorio")
    private String numero;

    @NotNull(message = "El importe es obligatorio")
    private BigDecimal importe;

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDate fechaEmision;
}