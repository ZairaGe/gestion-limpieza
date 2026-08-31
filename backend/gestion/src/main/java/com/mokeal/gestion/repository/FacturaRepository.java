package com.mokeal.gestion.repository;

import com.mokeal.gestion.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacturaRepository extends JpaRepository<Factura, Long> {
    Optional<Factura> findByServicio_Id(Long servicioId);

    @Query("SELECT COALESCE(SUM(f.importe), 0) FROM Factura f WHERE f.fechaEmision BETWEEN :inicio AND :fin")
BigDecimal sumarImportePorRangoFechas(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);
}