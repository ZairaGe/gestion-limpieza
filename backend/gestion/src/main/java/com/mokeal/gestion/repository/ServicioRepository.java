package com.mokeal.gestion.repository;

import com.mokeal.gestion.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    List<Servicio> findByFecha(LocalDate fecha);

    List<Servicio> findByEmpleados_Id(Long empleadoId);
}