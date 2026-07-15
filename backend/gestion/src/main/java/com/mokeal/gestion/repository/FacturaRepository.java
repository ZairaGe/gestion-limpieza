package com.mokeal.gestion.repository;

import com.mokeal.gestion.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {
    Optional<Factura> findByServicio_Id(Long servicioId);
}