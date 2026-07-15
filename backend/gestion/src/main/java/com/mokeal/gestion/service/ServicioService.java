package com.mokeal.gestion.service;

import com.mokeal.gestion.dto.ServicioRequest;
import com.mokeal.gestion.model.*;
import com.mokeal.gestion.repository.*;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final ClienteRepository clienteRepository;
    private final TarifaRepository tarifaRepository;
    private final EmpleadoRepository empleadoRepository;

    public ServicioService(ServicioRepository servicioRepository, ClienteRepository clienteRepository,
                            TarifaRepository tarifaRepository, EmpleadoRepository empleadoRepository) {
        this.servicioRepository = servicioRepository;
        this.clienteRepository = clienteRepository;
        this.tarifaRepository = tarifaRepository;
        this.empleadoRepository = empleadoRepository;
    }

    public List<Servicio> listarTodos() {
        return servicioRepository.findAll();
    }

    public Servicio buscarPorId(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con id: " + id));
    }

    public List<Servicio> buscarPorFecha(java.time.LocalDate fecha) {
        return servicioRepository.findByFecha(fecha);
    }

    public List<Servicio> buscarPorEmpleado(Long empleadoId) {
        return servicioRepository.findByEmpleados_Id(empleadoId);
    }

    public Servicio crear(ServicioRequest request) {
        Servicio servicio = construirDesdeRequest(new Servicio(), request);
        return servicioRepository.save(servicio);
    }

    public Servicio actualizar(Long id, ServicioRequest request) {
        Servicio servicio = buscarPorId(id);
        servicio = construirDesdeRequest(servicio, request);
        return servicioRepository.save(servicio);
    }

    public void eliminar(Long id) {
        Servicio servicio = buscarPorId(id);
        servicioRepository.delete(servicio);
    }

    private Servicio construirDesdeRequest(Servicio servicio, ServicioRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con id: " + request.getClienteId()));
        Tarifa tarifa = tarifaRepository.findById(request.getTarifaId())
                .orElseThrow(() -> new RuntimeException("Tarifa no encontrada con id: " + request.getTarifaId()));

        Set<Empleado> empleados = request.getEmpleadoIds().stream()
                .map(empId -> empleadoRepository.findById(empId)
                        .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id: " + empId)))
                .collect(Collectors.toSet());

        servicio.setCliente(cliente);
        servicio.setTarifa(tarifa);
        servicio.setDireccion(request.getDireccion());
        servicio.setFecha(request.getFecha());
        servicio.setHoraInicio(request.getHoraInicio());
        servicio.setHoraFin(request.getHoraFin());
        servicio.setEmpleados(new HashSet<>(empleados));

        return servicio;
    }
}