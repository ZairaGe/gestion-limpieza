package com.mokeal.gestion.service;

import com.mokeal.gestion.dto.ServicioRequest;
import com.mokeal.gestion.dto.ServicioResponse;
import com.mokeal.gestion.model.*;
import com.mokeal.gestion.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
            TarifaRepository tarifaRepository, EmpleadoRepository empleadoRepository,
            GeocodingService geocodingService) {
        this.servicioRepository = servicioRepository;
        this.clienteRepository = clienteRepository;
        this.tarifaRepository = tarifaRepository;
        this.empleadoRepository = empleadoRepository;
        this.geocodingService = geocodingService;
    }

    public List<ServicioResponse> listarTodos() {
        return servicioRepository
                .findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                        "id"))
                .stream()
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

    public List<ServicioResponse> buscarPorRangoFechas(LocalDate desde, LocalDate hasta) {
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

        double duracionTotal = request.getDuracionHoras();
        int numEmpleados = Math.max(1, empleados.size());
        long minutosReales = Math.round((duracionTotal / numEmpleados) * 60);

        servicio.setDuracionHoras(duracionTotal);
        servicio.setHoraInicio(request.getHoraInicio());
        servicio.setHoraFin(request.getHoraInicio().plusMinutes(minutosReales));
        servicio.setEmpleados(new HashSet<>(empleados));

        if (!empleados.isEmpty() && servicio.getEstado() != EstadoServicio.CANCELADO
                && servicio.getEstado() != EstadoServicio.COMPLETADO) {
            servicio.setEstado(EstadoServicio.CONFIRMADO);
        } else if (empleados.isEmpty() && servicio.getEstado() == null) {
            servicio.setEstado(EstadoServicio.PENDIENTE);
        }

        // Geocodificar la dirección y establecer las coordenadas
        double[] coordenadas = geocodingService.geocodificar(request.getDireccion());
        if (coordenadas != null) {
            servicio.setLatitud(coordenadas[0]);
            servicio.setLongitud(coordenadas[1]);
        }

        return servicio;
    }

    private ServicioResponse convertir(Servicio servicio) {
        double duracionTotalHoras = (servicio.getHoraFin().toSecondOfDay() - servicio.getHoraInicio().toSecondOfDay())
                / 3600.0;
        int numEmpleados = servicio.getEmpleados().size();
        double horasReales = (servicio.getHoraFin().toSecondOfDay() - servicio.getHoraInicio().toSecondOfDay())
                / 3600.0;

        Set<ServicioResponse.EmpleadoResumen> empleadosResumen = servicio.getEmpleados().stream()
                .map(e -> ServicioResponse.EmpleadoResumen.builder()
                        .id(e.getId())
                        .nombre(e.getNombre())
                        .horasAsignadas(horasReales)
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
                .duracionHoras(servicio.getDuracionHoras())
                .longitud(servicio.getLongitud())
                .empleados(empleadosResumen)
                .pagado(servicio.isPagado())
                .fechaPago(servicio.getFechaPago())
                .importe(calcularCoste(servicio))
                .build();
    }

    public void guardarDirecto(Servicio servicio) {
        servicioRepository.save(servicio);
    }

    public List<ServicioResponse> buscarPorClienteYRango(Long clienteId, LocalDate desde, LocalDate hasta) {
        return servicioRepository.findByClienteIdAndFechaBetween(clienteId, desde, hasta).stream()
                .map(this::convertir)
                .collect(Collectors.toList());
    }

    public ServicioResponse marcarPagado(Long id) {
        Servicio s = buscarEntidad(id);
        s.setPagado(true);
        s.setFechaPago(LocalDate.now());
        return convertir(servicioRepository.save(s));
    }

    public ServicioResponse marcarPendiente(Long id) {
        Servicio s = buscarEntidad(id);
        s.setPagado(false);
        s.setFechaPago(null);
        return convertir(servicioRepository.save(s));
    }

    private BigDecimal calcularCoste(Servicio servicio) {
        Tarifa tarifa = servicio.getTarifa();
        if (tarifa.getPrecioFijo() != null) {
            return tarifa.getPrecioFijo();
        }
        double duracionHoras = (servicio.getHoraFin().toSecondOfDay() - servicio.getHoraInicio().toSecondOfDay())
                / 3600.0;
        return tarifa.getPrecioHora().multiply(BigDecimal.valueOf(duracionHoras));
    }

    public ServicioResponse cambiarEstado(Long id, EstadoServicio nuevoEstado) {
        Servicio s = buscarEntidad(id);
        s.setEstado(nuevoEstado);
        return convertir(servicioRepository.save(s));
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 5 0 * * *")
    public void completarServiciosPasados() {
        LocalDate ayer = LocalDate.now().minusDays(1);
        List<Servicio> confirmadosPasados = servicioRepository.findByFechaBetween(LocalDate.of(2000, 1, 1), ayer)
                .stream()
                .filter(s -> s.getEstado() == EstadoServicio.CONFIRMADO)
                .collect(Collectors.toList());

        for (Servicio s : confirmadosPasados) {
            s.setEstado(EstadoServicio.COMPLETADO);
            servicioRepository.save(s);
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 10 0 * * *")
    public void eliminarServiciosAntiguos() {
        LocalDate limite = LocalDate.now().minusMonths(2);
        List<Servicio> antiguos = servicioRepository.findByFechaBefore(limite);
        servicioRepository.deleteAll(antiguos);
    }
}