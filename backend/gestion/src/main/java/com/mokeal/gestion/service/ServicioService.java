package com.mokeal.gestion.service;

import com.mokeal.gestion.dto.ServicioRequest;
import com.mokeal.gestion.dto.ServicioResponse;
import com.mokeal.gestion.model.*;
import com.mokeal.gestion.repository.*;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
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
    private final GeocodingService geocodingService;

    public ServicioService(ServicioRepository servicioRepository, ClienteRepository clienteRepository,
                            TarifaRepository tarifaRepository, EmpleadoRepository empleadoRepository, GeocodingService geocodingService) {
        this.servicioRepository = servicioRepository;
        this.clienteRepository = clienteRepository;
        this.tarifaRepository = tarifaRepository;
        this.empleadoRepository = empleadoRepository;
        this.geocodingService = geocodingService;
    }

    public List<ServicioResponse> listarTodos() {
        return servicioRepository.findAll().stream()
                .map(this::convertir)
                .collect(Collectors.toList());
    }

    public ServicioResponse buscarPorId(Long id) {
        return convertir(buscarEntidad(id));
    }

    public List<ServicioResponse> buscarPorFecha(LocalDate fecha) {
        return servicioRepository.findByFecha(fecha).stream()
                .map(this::convertir)
                .collect(Collectors.toList());
    }

    public List<ServicioResponse> buscarPorRangoFechas (LocalDate desde, LocalDate hasta) {
        return servicioRepository.findByFechaBetween(desde, hasta).stream()
                .map(this::convertir)
                .collect(Collectors.toList());
    }

    public List<ServicioResponse> buscarPorEmpleado(Long empleadoId) {
        return servicioRepository.findByEmpleados_Id(empleadoId).stream()
                .map(this::convertir)
                .collect(Collectors.toList());
    }

    private Servicio buscarEntidad(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con id: " + id));
    }

    public ServicioResponse crear(ServicioRequest request) {
        Servicio servicio = construirDesdeRequest(new Servicio(), request);
        return convertir(servicioRepository.save(servicio));
    }

    public ServicioResponse actualizar(Long id, ServicioRequest request) {
        Servicio servicio = buscarEntidad(id);
        servicio = construirDesdeRequest(servicio, request);
        return convertir(servicioRepository.save(servicio));
    }

    public void eliminar(Long id) {
        Servicio servicio = buscarEntidad(id);
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

        // Geocodificar la dirección y establecer las coordenadas
        double[] coordenadas = geocodingService.geocodificar(request.getDireccion());
        if (coordenadas != null) {
            servicio.setLatitud(coordenadas[0]);
            servicio.setLongitud(coordenadas[1]);
        }

        return servicio;
    }

    private ServicioResponse convertir(Servicio servicio) {
        Set<ServicioResponse.EmpleadoResumen> empleadosResumen = servicio.getEmpleados().stream()
                .map(e -> ServicioResponse.EmpleadoResumen.builder()
                        .id(e.getId())
                        .nombre(e.getNombre())
                        .build())
                .collect(Collectors.toSet());

        return ServicioResponse.builder()
                .id(servicio.getId())
                .clienteId(servicio.getCliente().getId())
                .clienteNombre(servicio.getCliente().getNombre())
                .tarifaId(servicio.getTarifa().getId())
                .tarifaTipoServicio(servicio.getTarifa().getTipoServicio().name())
                .tarifaZona(servicio.getTarifa().getZona().name())
                .direccion(servicio.getDireccion())
                .fecha(servicio.getFecha())
                .horaInicio(servicio.getHoraInicio())
                .horaFin(servicio.getHoraFin())
                .estado(servicio.getEstado())
                .latitud(servicio.getLatitud())
                .longitud(servicio.getLongitud())
                .empleados(empleadosResumen)
                .build();
    }
}