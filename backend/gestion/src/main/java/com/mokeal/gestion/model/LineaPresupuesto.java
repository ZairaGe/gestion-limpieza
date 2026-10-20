package com.mokeal.gestion.model;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.math.BigDecimal;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LineaPresupuesto {
    private String concepto;
    private Double horas;
    private BigDecimal precioHora;
    private BigDecimal subtotal;
}