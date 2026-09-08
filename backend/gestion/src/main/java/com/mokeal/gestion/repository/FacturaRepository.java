package com.mokeal.gestion.repository;

import com.mokeal.gestion.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    List<Factura> findByCliente_Id(Long clienteId);

    @Query("SELECT COUNT(f) FROM Factura f WHERE f.numero LIKE CONCAT('FAC-', :anio, '-%')")
    long contarPorAnio(@Param("anio") String anio);

    @Query("SELECT COALESCE(SUM(f.importe), 0) FROM Factura f WHERE f.fechaEmision BETWEEN :inicio AND :fin")
    BigDecimal sumarImportePorRangoFechas(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);
}