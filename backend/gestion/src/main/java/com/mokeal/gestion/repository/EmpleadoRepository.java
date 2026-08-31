package com.mokeal.gestion.repository;

import com.mokeal.gestion.model.Empleado;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    long countByActivoTrue();

List<Empleado> findByActivoTrue();
}