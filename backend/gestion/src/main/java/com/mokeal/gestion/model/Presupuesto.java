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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ElementCollection
    @CollectionTable(name = "presupuesto_linea", joinColumns = @JoinColumn(name = "presupuesto_id"))
    @Builder.Default
    private List<LineaPresupuesto> lineas = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Builder.Default
    private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoPresupuesto estado = EstadoPresupuesto.PENDIENTE;

    @Column(nullable = false)
    private LocalDate fechaEmision;
}