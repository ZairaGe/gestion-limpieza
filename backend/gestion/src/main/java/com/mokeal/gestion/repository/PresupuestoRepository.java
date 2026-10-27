package com.mokeal.gestion.repository;

import com.mokeal.gestion.model.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    @Query("SELECT COUNT(p) FROM Presupuesto p WHERE p.numero LIKE CONCAT('PRE-', :anio, '-%')")
    long contarPorAnio(@Param("anio") String anio);
}