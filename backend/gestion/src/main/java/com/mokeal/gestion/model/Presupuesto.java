package com.mokeal.gestion.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "presupuesto")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Presupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String nombre;

    @Column(length = 30)
    private String telefono;

    @Column(length = 120)
    private String correo;

    @Column(length = 255)
    private String direccion;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ElementCollection
    @CollectionTable(name = "presupuesto_linea", joinColumns = @JoinColumn(name = "presupuesto_id"))
    @Builder.Default
    private List<LineaPresupuesto> lineas = new ArrayList<>();

    @Column(length = 500)
    private String notas;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Builder.Default
    private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;

    private BigDecimal ivaPorcentaje;

    private BigDecimal ivaImporte;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    private LocalDate fechaEmision;
}