package com.mokeal.gestion.dto;

import com.mokeal.gestion.model.DiaSemana;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
public class ServicioRecurrenteRequest {

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    @NotNull(message = "La tarifa es obligatoria")
    private Long tarifaId;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La duración es obligatoria")
    @Positive(message = "La duración debe ser mayor que 0")
    private Double duracionHoras;

    @NotEmpty(message = "Debe seleccionar al menos un día de la semana")
    private Set<DiaSemana> diasSemana;

    private Set<Long> empleadoIds = new java.util.HashSet<>();

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;
}