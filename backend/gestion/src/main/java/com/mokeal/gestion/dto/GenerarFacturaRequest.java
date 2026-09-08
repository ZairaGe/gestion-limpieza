package com.mokeal.gestion.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class GenerarFacturaRequest {

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    @NotNull(message = "La fecha desde es obligatoria")
    private LocalDate desde;

    @NotNull(message = "La fecha hasta es obligatoria")
    private LocalDate hasta;

    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDate fechaEmision;
}