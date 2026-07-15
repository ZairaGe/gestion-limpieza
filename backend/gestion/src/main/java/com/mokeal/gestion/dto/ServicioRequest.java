package com.mokeal.gestion.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
public class ServicioRequest {

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    @NotNull(message = "La tarifa es obligatoria")
    private Long tarifaId;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    @NotEmpty(message = "Debe asignar al menos un empleado")
    private Set<Long> empleadoIds;
}