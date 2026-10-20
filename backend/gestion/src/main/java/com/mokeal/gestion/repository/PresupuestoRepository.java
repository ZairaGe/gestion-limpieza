package com.mokeal.gestion.repository;

import com.mokeal.gestion.model.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    List<Presupuesto> findByCliente_Id(Long clienteId);

    @Query("SELECT COUNT(p) FROM Presupuesto p WHERE p.numero LIKE CONCAT('PRE-', :anio, '-%')")
    long contarPorAnio(@Param("anio") String anio);
}