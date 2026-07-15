package com.mokeal.gestion.repository;

import com.mokeal.gestion.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}