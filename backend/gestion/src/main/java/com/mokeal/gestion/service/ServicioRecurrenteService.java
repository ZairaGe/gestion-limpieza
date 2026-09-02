package com.mokeal.gestion.service;

import com.mokeal.gestion.dto.ServicioRecurrenteRequest;
import com.mokeal.gestion.model.*;
import com.mokeal.gestion.repository.*;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ServicioRecurrenteService {

    private final ServicioRecurrenteRepository servicioRecurrenteRepository;
    private final ServicioService servicioService;
    private final ClienteRepository clienteRepository;
    private final TarifaRepository tarifaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final GeocodingService geocodingService;

    public ServicioRecurrenteService(ServicioRecurrenteRepository servicioRecurrenteRepository,
            ServicioService servicioService,
            ClienteRepository clienteRepository,
            TarifaRepository tarifaRepository,
            EmpleadoRepository empleadoRepository,
            GeocodingService geocodingService) {
        this.servicioRecurrenteRepository = servicioRecurrenteRepository;
        this.servicioService = servicioService;
        this.clienteRepository = clienteRepository;
        this.tarifaRepository = tarifaRepository;
        this.empleadoRepository = empleadoRepository;
        this.geocodingService = geocodingService;
    }

    public List<ServicioRecurrente> listarTodas() {
        return servicioRecurrenteRepository.findAll();
    }

    public int crearYGenerar(ServicioRecurrenteRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        Tarifa tarifa = tarifaRepository.findById(request.getTarifaId())
                .orElseThrow(() -> new RuntimeException("Tarifa no encontrada"));

        Set<Empleado> empleados = request.getEmpleadoIds().stream()
                .map(id -> empleadoRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id: " + id)))
                .collect(Collectors.toSet());

        long minutos = Math.round(request.getDuracionHoras() * 60);

        ServicioRecurrente recurrente = ServicioRecurrente.builder()
                .cliente(cliente)
                .tarifa(tarifa)
                .direccion(request.getDireccion())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraInicio().plusMinutes(minutos))
                .diasSemana(new HashSet<>(request.getDiasSemana()))
                .empleados(new HashSet<>(empleados))
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .build();

        servicioRecurrenteRepository.save(recurrente);

        double[] coords = geocodingService.geocodificar(request.getDireccion());

        return generarServicios(recurrente, coords);
    }

    private int generarServicios(ServicioRecurrente recurrente, double[] coords) {
        int contador = 0;
        LocalDate fecha = recurrente.getFechaInicio();

        while (!fecha.isAfter(recurrente.getFechaFin())) {
            DiaSemana diaActual = convertirDiaSemana(fecha.getDayOfWeek());

            if (recurrente.getDiasSemana().contains(diaActual)) {
                Servicio servicio = new Servicio();
                servicio.setCliente(recurrente.getCliente());
                servicio.setTarifa(recurrente.getTarifa());
                servicio.setDireccion(recurrente.getDireccion());
                servicio.setFecha(fecha);
                servicio.setHoraInicio(recurrente.getHoraInicio());
                servicio.setHoraFin(recurrente.getHoraFin());
                servicio.setEmpleados(new HashSet<>(recurrente.getEmpleados()));
                servicio.setLatitud(coords != null ? coords[0] : null);
                servicio.setLongitud(coords != null ? coords[1] : null);

                servicioService.guardarDirecto(servicio);
                contador++;
            }

            fecha = fecha.plusDays(1);
        }

        return contador;
    }

    private DiaSemana convertirDiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }
}