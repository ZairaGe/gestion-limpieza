
package com.mokeal.gestion.repository;

import com.mokeal.gestion.model.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    Optional<Presupuesto> findFirstByNumeroStartingWithOrderByNumeroDesc(String prefijo);
}
