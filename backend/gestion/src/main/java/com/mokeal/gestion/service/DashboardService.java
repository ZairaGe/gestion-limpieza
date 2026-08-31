package com.mokeal.gestion.service;

import com.mokeal.gestion.dto.DashboardResponse;
import com.mokeal.gestion.dto.DashboardResponse.*;
import com.mokeal.gestion.model.Empleado;
import com.mokeal.gestion.model.Servicio;
import com.mokeal.gestion.model.Tarifa;
import com.mokeal.gestion.repository.*;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ServicioRepository servicioRepository;
    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TarifaRepository tarifaRepository;

    public DashboardService(ServicioRepository servicioRepository, FacturaRepository facturaRepository,
                             ClienteRepository clienteRepository, EmpleadoRepository empleadoRepository,
                             TarifaRepository tarifaRepository) {
        this.servicioRepository = servicioRepository;
        this.facturaRepository = facturaRepository;
        this.clienteRepository = clienteRepository;
        this.empleadoRepository = empleadoRepository;
        this.tarifaRepository = tarifaRepository;
    }

    public DashboardResponse obtenerResumen() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioSemana = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate finSemana = inicioSemana.plusDays(6);

        List<Servicio> serviciosHoy = servicioRepository.findByFecha(hoy);
        List<Empleado> empleadosActivos = empleadoRepository.findByActivoTrue();
        List<Tarifa> tarifas = tarifaRepository.findAll();

        return DashboardResponse.builder()
                .serviciosHoy(serviciosHoy.size())
                .ingresosSemana(facturaRepository.sumarImportePorRangoFechas(inicioSemana, finSemana))
                .trabajadoresActivos(empleadoRepository.countByActivoTrue())
                .pendientesAsignar(servicioRepository.contarSinEmpleadosAsignados())
                .agendaHoy(serviciosHoy.stream().map(this::convertirServicio).collect(Collectors.toList()))
                .trabajadores(empleadosActivos.stream().map(this::convertirEmpleado).collect(Collectors.toList()))
                .tarifasActivas(tarifas.stream().map(this::convertirTarifa).collect(Collectors.toList()))
                .resumenSemanal(ResumenSemanal.builder()
                        .totalServicios(servicioRepository.countByFechaBetween(inicioSemana, finSemana))
                        .pendientes(servicioRepository.contarSinEmpleadosAsignadosEnRango(inicioSemana, finSemana))
                        .build())
                .build();
    }

    private ServicioResumen convertirServicio(Servicio servicio) {
        return ServicioResumen.builder()
                .id(servicio.getId())
                .clienteNombre(servicio.getCliente().getNombre())
                .horaInicio(servicio.getHoraInicio())
                .horaFin(servicio.getHoraFin())
                .direccion(servicio.getDireccion())
                .estado(servicio.getEstado().name())
                .build();
    }

    private EmpleadoResumen convertirEmpleado(Empleado empleado) {
        return EmpleadoResumen.builder()
                .id(empleado.getId())
                .nombre(empleado.getNombre())
                .build();
    }

    private TarifaResumen convertirTarifa(Tarifa tarifa) {
        return TarifaResumen.builder()
                .tipoServicio(tarifa.getTipoServicio().name())
                .zona(tarifa.getZona().name())
                .precioHora(tarifa.getPrecioHora())
                .precioFijo(tarifa.getPrecioFijo())
                .build();
    }
}