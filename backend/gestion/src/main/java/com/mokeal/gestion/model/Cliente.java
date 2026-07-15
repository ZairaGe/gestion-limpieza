package com.mokeal.gestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "cliente")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 20)
    private String telefono;

    @Email(message = "El email no es válido")
    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TipoCliente tipo = TipoCliente.PARTICULAR;
}