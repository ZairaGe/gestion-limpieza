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

    @NotNull(message = "El tipo de servicio es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoServicio tipoServicio;

    @NotNull(message = "La zona es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Zona zona;

    @DecimalMin(value = "0.0", inclusive = false, message = "El precio por hora debe ser mayor que 0")
    private BigDecimal precioHora;

    @DecimalMin(value = "0.0", inclusive = false, message = "El precio fijo debe ser mayor que 0")
    private BigDecimal precioFijo;
}