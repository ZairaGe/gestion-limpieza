package com.mokeal.gestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "servicio")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @NotNull(message = "La tarifa es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarifa_id", nullable = false)
    private Tarifa tarifa;

    @NotBlank(message = "La dirección es obligatoria")
    @Column(nullable = false, length = 255)
    private String direccion;

    private Double latitud;

    private Double longitud;

    @NotNull(message = "La fecha es obligatoria")
    @Column(nullable = false)
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria")
    @Column(nullable = false)
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    @Column(nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EstadoServicio estado = EstadoServicio.PENDIENTE;

    @ManyToMany
    @JoinTable(
        name = "servicio_empleado",
        joinColumns = @JoinColumn(name = "servicio_id"),
        inverseJoinColumns = @JoinColumn(name = "empleado_id")
    )
    @Builder.Default
    private Set<Empleado> empleados = new HashSet<>();
}