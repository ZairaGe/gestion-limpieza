package com.mokeal.gestion.dto;

import com.mokeal.gestion.model.DiaSemana;
import com.mokeal.gestion.model.EstadoServicio;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Data
public class ServicioRequest {

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    private Long tarifaId;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La duración es obligatoria")
    @Positive(message = "La duración debe ser mayor que 0")
    private Double duracionHoras;

    private BigDecimal precio;

    private String notas;

    private EstadoServicio estado;

    private Set<Long> empleadoIds = new HashSet<>();

    private Set<DiaSemana> diasSemana = new HashSet<>();

    private LocalDate repetirHasta;
}