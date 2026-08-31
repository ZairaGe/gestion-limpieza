package com.mokeal.gestion.repository;

import com.mokeal.gestion.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;


public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    List<Servicio> findByFecha(LocalDate fecha);

    List<Servicio> findByEmpleados_Id(Long empleadoId);
    long countByFecha(LocalDate fecha);


long countByFechaBetween(LocalDate inicio, LocalDate fin);

@Query("SELECT COUNT(s) FROM Servicio s WHERE s.empleados IS EMPTY")
long contarSinEmpleadosAsignados();

@Query("SELECT COUNT(s) FROM Servicio s WHERE s.empleados IS EMPTY AND s.fecha BETWEEN :inicio AND :fin")
long contarSinEmpleadosAsignadosEnRango(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);
}