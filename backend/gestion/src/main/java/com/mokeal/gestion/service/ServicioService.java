package com.mokeal.gestion.service;

import com.mokeal.gestion.dto.ServicioRequest;
import com.mokeal.gestion.dto.ServicioResponse;
import com.mokeal.gestion.model.*;
import com.mokeal.gestion.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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

    // ---------- Consultas ----------

    public List<ServicioResponse> listarTodos() {
        return servicioRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
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

    public List<ServicioResponse> buscarPorClienteYRango(Long clienteId, LocalDate desde, LocalDate hasta) {
        return servicioRepository.findByClienteIdAndFechaBetween(clienteId, desde, hasta).stream()
                .map(this::convertir)
                .collect(Collectors.toList());
    }

    private Servicio buscarEntidad(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con id: " + id));
    }

    // ---------- Crear / actualizar / eliminar ----------

    public List<ServicioResponse> crear(ServicioRequest request) {
        boolean recurrente = request.getDiasSemana() != null
                && !request.getDiasSemana().isEmpty()
                && request.getRepetirHasta() != null;

        List<LocalDate> fechas = new ArrayList<>();
        if (recurrente) {
            if (request.getRepetirHasta().isAfter(request.getFecha().plusYears(1))) {
                throw new RuntimeException("El rango de repetición no puede superar un año");
            }
            for (LocalDate f = request.getFecha(); !f.isAfter(request.getRepetirHasta()); f = f.plusDays(1)) {
                if (request.getDiasSemana().contains(aDiaSemana(f.getDayOfWeek()))) {
                    fechas.add(f);
                }
            }
            if (fechas.isEmpty()) {
                throw new RuntimeException("Ningún día seleccionado cae dentro del rango de fechas");
            }
        } else {
            fechas.add(request.getFecha());
        }

        // Se geocodifica una sola vez aunque se creen muchos servicios
        double[] coordenadas = geocodingService.geocodificar(request.getDireccion());

        List<Servicio> guardados = new ArrayList<>();
        for (LocalDate fecha : fechas) {
            Servicio servicio = construirDesdeRequest(new Servicio(), request, fecha, coordenadas);
            servicio.setRecurrente(recurrente);
            guardados.add(servicioRepository.save(servicio));
        }

        return guardados.stream().map(this::convertir).collect(Collectors.toList());
    }

    public ServicioResponse actualizar(Long id, ServicioRequest request) {
        Servicio servicio = buscarEntidad(id);
        double[] coordenadas = geocodingService.geocodificar(request.getDireccion());
        servicio = construirDesdeRequest(servicio, request, request.getFecha(), coordenadas);
        return convertir(servicioRepository.save(servicio));
    }

    public void eliminar(Long id) {
        servicioRepository.delete(buscarEntidad(id));
    }

    public void guardarDirecto(Servicio servicio) {
        servicioRepository.save(servicio);
    }

    // ---------- Pagos y estado ----------

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

    public ServicioResponse cambiarEstado(Long id, EstadoServicio nuevoEstado) {
        Servicio s = buscarEntidad(id);
        s.setEstado(nuevoEstado);
        return convertir(servicioRepository.save(s));
    }

    // ---------- Tareas programadas ----------

    @Scheduled(cron = "0 5 0 * * *")
    public void completarServiciosPasados() {
        LocalDate ayer = LocalDate.now().minusDays(1);
        List<Servicio> confirmadosPasados = servicioRepository
                .findByFechaBetween(LocalDate.of(2000, 1, 1), ayer).stream()
                .filter(s -> s.getEstado() == EstadoServicio.CONFIRMADO)
                .collect(Collectors.toList());

        for (Servicio s : confirmadosPasados) {
            s.setEstado(EstadoServicio.COMPLETADO);
            servicioRepository.save(s);
        }
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void eliminarServiciosAntiguos() {
        LocalDate limite = LocalDate.now().minusMonths(2);
        servicioRepository.deleteAll(servicioRepository.findByFechaBefore(limite));
    }

    // ---------- Conversión ----------

    private Servicio construirDesdeRequest(Servicio servicio, ServicioRequest request,
            LocalDate fecha, double[] coordenadas) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con id: " + request.getClienteId()));

        Tarifa tarifa = null;
        if (request.getTarifaId() != null) {
            tarifa = tarifaRepository.findById(request.getTarifaId())
                    .orElseThrow(() -> new RuntimeException("Tarifa no encontrada con id: " + request.getTarifaId()));
        }

        Set<Long> ids = request.getEmpleadoIds() != null ? request.getEmpleadoIds() : Collections.emptySet();
        Set<Empleado> empleados = ids.stream()
                .map(empId -> empleadoRepository.findById(empId)
                        .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id: " + empId)))
                .collect(Collectors.toSet());

        servicio.setCliente(cliente);
        servicio.setTarifa(tarifa);
        servicio.setDireccion(request.getDireccion());
        servicio.setFecha(fecha);
        servicio.setPrecioManual(request.getPrecio());
        servicio.setNotas(request.getNotas() != null && !request.getNotas().isBlank()
                ? request.getNotas().trim()
                : null);

        // La duración contratada no cambia; el horario real se acorta si trabajan
        // varios a la vez
        double duracionTotal = request.getDuracionHoras();
        int numEmpleados = Math.max(1, empleados.size());
        long minutosReales = Math.round((duracionTotal / numEmpleados) * 60);

        servicio.setDuracionHoras(duracionTotal);
        servicio.setHoraInicio(request.getHoraInicio());
        servicio.setHoraFin(request.getHoraInicio().plusMinutes(minutosReales));
        servicio.setEmpleados(new HashSet<>(empleados));

        if (servicio.getEstado() == null) {
            servicio.setEstado(EstadoServicio.PENDIENTE);
        }
        if (request.getEstado() != null) {
            servicio.setEstado(request.getEstado());
        } else if (!empleados.isEmpty()
                && servicio.getEstado() != EstadoServicio.CANCELADO
                && servicio.getEstado() != EstadoServicio.COMPLETADO) {
            servicio.setEstado(EstadoServicio.CONFIRMADO);
        }

        if (coordenadas != null) {
            servicio.setLatitud(coordenadas[0]);
            servicio.setLongitud(coordenadas[1]);
        }

        return servicio;
    }

    private DiaSemana aDiaSemana(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }

    private ServicioResponse convertir(Servicio servicio) {
        double horasReales = (servicio.getHoraFin().toSecondOfDay() - servicio.getHoraInicio().toSecondOfDay())
                / 3600.0;

        Set<ServicioResponse.EmpleadoResumen> empleadosResumen = servicio.getEmpleados().stream()
                .map(e -> ServicioResponse.EmpleadoResumen.builder()
                        .id(e.getId())
                        .nombre(e.getNombre())
                        .horasAsignadas(horasReales)
                        .build())
                .collect(Collectors.toSet());

        Tarifa tarifa = servicio.getTarifa();

        return ServicioResponse.builder()
                .id(servicio.getId())
                .clienteId(servicio.getCliente().getId())
                .clienteNombre(servicio.getCliente().getNombre())
                .tarifaId(tarifa != null ? tarifa.getId() : null)
                .tarifaNombre(tarifa != null ? tarifa.getNombre() : null)
                .tarifaTipoServicio(
                        tarifa != null && tarifa.getTipoServicio() != null ? tarifa.getTipoServicio().name() : null)
                .tarifaZona(tarifa != null && tarifa.getZona() != null ? tarifa.getZona().name() : null)
                .direccion(servicio.getDireccion())
                .fecha(servicio.getFecha())
                .horaInicio(servicio.getHoraInicio())
                .horaFin(servicio.getHoraFin())
                .duracionHoras(servicio.getDuracionHoras())
                .estado(servicio.getEstado())
                .latitud(servicio.getLatitud())
                .longitud(servicio.getLongitud())
                .empleados(empleadosResumen)
                .pagado(servicio.isPagado())
                .fechaPago(servicio.getFechaPago())
                .importe(servicio.calcularImporte())
                .recurrente(servicio.isRecurrente())
                .precioManual(servicio.getPrecioManual())
                .facturado(servicio.getFactura() != null)
                .notas(servicio.getNotas())
                .build();
    }
}