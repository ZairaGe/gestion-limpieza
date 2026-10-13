package com.mokeal.gestion.service;

import com.mokeal.gestion.dto.DashboardResponse;
import com.mokeal.gestion.dto.DashboardResponse.DiaIngreso;
import com.mokeal.gestion.dto.DashboardResponse.ServicioHoy;
import com.mokeal.gestion.model.*;
import com.mokeal.gestion.repository.EmpleadoRepository;
import com.mokeal.gestion.repository.ServicioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ServicioRepository servicioRepository;
    private final EmpleadoRepository empleadoRepository;

    public DashboardService(ServicioRepository servicioRepository, EmpleadoRepository empleadoRepository) {
        this.servicioRepository = servicioRepository;
        this.empleadoRepository = empleadoRepository;
    }

    public DashboardResponse obtenerResumen() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioSemana = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate finSemana = inicioSemana.plusDays(6);

        List<Servicio> semana = servicioRepository.findByFechaBetween(inicioSemana, finSemana).stream()
                .filter(s -> s.getEstado() != EstadoServicio.CANCELADO)
                .collect(Collectors.toList());

        List<Servicio> serviciosHoy = semana.stream()
                .filter(s -> s.getFecha().equals(hoy))
                .sorted(Comparator.comparing(Servicio::getHoraInicio))
                .collect(Collectors.toList());

        List<Empleado> activos = empleadoRepository.findByActivoTrue();
        Set<Long> ocupadosHoy = serviciosHoy.stream()
                .flatMap(s -> s.getEmpleados().stream())
                .map(Empleado::getId)
                .collect(Collectors.toSet());
        long sinServicioHoy = activos.stream().filter(e -> !ocupadosHoy.contains(e.getId())).count();

        List<DiaIngreso> dias = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate fecha = inicioSemana.plusDays(i);
            List<Servicio> delDia = semana.stream()
                    .filter(s -> s.getFecha().equals(fecha))
                    .collect(Collectors.toList());
            dias.add(DiaIngreso.builder()
                    .fecha(fecha)
                    .hoy(fecha.equals(hoy))
                    .previsto(sumar(delDia, false))
                    .cobrado(sumar(delDia, true))
                    .build());
        }

        List<ServicioHoy> agenda = serviciosHoy.stream()
                .map(s -> ServicioHoy.builder()
                        .id(s.getId())
                        .clienteNombre(s.getCliente().getNombre())
                        .tipoServicio(s.getTarifa().getTipoServicio().name())
                        .horaInicio(s.getHoraInicio())
                        .horaFin(s.getHoraFin())
                        .estado(s.getEstado().name())
                        .empleados(s.getEmpleados().stream().map(Empleado::getNombre).sorted().collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        return DashboardResponse.builder()
                .ingresosSemana(sumar(semana, true))
                .previstoSemana(sumar(semana, false))
                .serviciosHoy(serviciosHoy.size())
                .trabajadoresActivos(activos.size())
                .sinServicioHoy(sinServicioHoy)
                .ingresosPorDia(dias)
                .agendaHoy(agenda)
                .build();
    }

    private BigDecimal sumar(List<Servicio> servicios, boolean soloPagados) {
        return servicios.stream()
                .filter(s -> !soloPagados || s.isPagado())
                .map(this::calcularCoste)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularCoste(Servicio servicio) {
        Tarifa tarifa = servicio.getTarifa();
        if (tarifa.getPrecioFijo() != null) return tarifa.getPrecioFijo();
        if (tarifa.getPrecioHora() == null) return BigDecimal.ZERO;
        double duracion = servicio.getDuracionHoras() != null
                ? servicio.getDuracionHoras()
                : (servicio.getHoraFin().toSecondOfDay() - servicio.getHoraInicio().toSecondOfDay()) / 3600.0;
        return tarifa.getPrecioHora().multiply(BigDecimal.valueOf(duracion));
    }
}