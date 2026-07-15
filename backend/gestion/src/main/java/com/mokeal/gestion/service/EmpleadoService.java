package com.mokeal.gestion.service;

import com.mokeal.gestion.model.Empleado;
import com.mokeal.gestion.repository.EmpleadoRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;

    public EmpleadoService(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    public List<Empleado> listarTodos() {
        return empleadoRepository.findAll();
    }

    public Empleado buscarPorId(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id: " + id));
    }

    public Empleado crear(Empleado empleado) {
        return empleadoRepository.save(empleado);
    }

    public Empleado actualizar(Long id, Empleado datosNuevos) {
        Empleado empleado = buscarPorId(id);
        empleado.setNombre(datosNuevos.getNombre());
        empleado.setTelefono(datosNuevos.getTelefono());
        empleado.setActivo(datosNuevos.isActivo());
        return empleadoRepository.save(empleado);
    }

    public void eliminar(Long id) {
        Empleado empleado = buscarPorId(id);
        empleadoRepository.delete(empleado);
    }
}