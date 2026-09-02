package com.mokeal.gestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "servicio_recurrente")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ServicioRecurrente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarifa_id", nullable = false)
    private Tarifa tarifa;

    @Column(nullable = false, length = 255)
    private String direccion;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @ElementCollection(targetClass = DiaSemana.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "servicio_recurrente_dias", joinColumns = @JoinColumn(name = "servicio_recurrente_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana")
    @Builder.Default
    private Set<DiaSemana> diasSemana = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "servicio_recurrente_empleado",
        joinColumns = @JoinColumn(name = "servicio_recurrente_id"),
        inverseJoinColumns = @JoinColumn(name = "empleado_id")
    )
    @Builder.Default
    private Set<Empleado> empleados = new HashSet<>();

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Builder.Default
    @Column(nullable = false)
    private boolean activa = true;
}