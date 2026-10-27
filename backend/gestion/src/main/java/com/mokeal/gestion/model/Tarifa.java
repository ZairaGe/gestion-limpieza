package com.mokeal.gestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tarifa")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    // Ya no se usan en la interfaz; se dejan opcionales para no perder los datos antiguos
    @Enumerated(EnumType.STRING)
    private TipoServicio tipoServicio;

    @Enumerated(EnumType.STRING)
    private Zona zona;

    @DecimalMin(value = "0.0", inclusive = false, message = "El precio por hora debe ser mayor que 0")
    private BigDecimal precioHora;

    @DecimalMin(value = "0.0", inclusive = false, message = "El precio fijo debe ser mayor que 0")
    private BigDecimal precioFijo;
}