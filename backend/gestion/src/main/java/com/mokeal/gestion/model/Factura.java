package com.mokeal.gestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "factura")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El servicio es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false, unique = true)
    private Servicio servicio;

    @NotBlank(message = "El número de factura es obligatorio")
    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @NotNull(message = "El importe es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El importe debe ser mayor que 0")
    @Column(nullable = false)
    private BigDecimal importe;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private EstadoFactura estado = EstadoFactura.PENDIENTE;

    @NotNull(message = "La fecha de emisión es obligatoria")
    @Column(nullable = false)
    private LocalDate fechaEmision;
}