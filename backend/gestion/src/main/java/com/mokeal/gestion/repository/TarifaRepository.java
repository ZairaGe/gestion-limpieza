package com.mokeal.gestion.repository;

import com.mokeal.gestion.model.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
}