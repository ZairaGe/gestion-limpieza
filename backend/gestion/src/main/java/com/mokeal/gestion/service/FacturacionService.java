package com.mokeal.gestion.service;

import com.mokeal.gestion.dto.FacturacionPendienteResponse;
import com.mokeal.gestion.dto.FacturacionPendienteResponse.ServicioPendiente;
import com.mokeal.gestion.dto.GenerarFacturaRequest;
import com.mokeal.gestion.model.*;
import com.mokeal.gestion.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FacturacionService {

    private final ServicioRepository servicioRepository;
    private final ClienteRepository clienteRepository;
    private final FacturaRepository facturaRepository;

    public FacturacionService(ServicioRepository servicioRepository, ClienteRepository clienteRepository,
                               FacturaRepository facturaRepository) {
        this.servicioRepository = servicioRepository;
        this.clienteRepository = clienteRepository;
        this.facturaRepository = facturaRepository;
    }

    public FacturacionPendienteResponse obtenerPendientes(Long clienteId, LocalDate desde, LocalDate hasta) {
        List<Servicio> servicios = servicioRepository
                .findByClienteIdAndFechaBetweenAndFacturaIsNull(clienteId, desde, hasta);

        List<ServicioPendiente> resumen = servicios.stream()
                .map(s -> ServicioPendiente.builder()
                        .id(s.getId())
                        .fecha(s.getFecha())
                        .horaInicio(s.getHoraInicio())
                        .horaFin(s.getHoraFin())
                        .direccion(s.getDireccion())
                        .importe(calcularCoste(s))
                        .build())
                .collect(Collectors.toList());

        BigDecimal total = resumen.stream()
                .map(ServicioPendiente::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return FacturacionPendienteResponse.builder()
                .servicios(resumen)
                .totalImporte(total)
                .build();
    }

    public Factura generarFactura(GenerarFacturaRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        List<Servicio> servicios = servicioRepository
                .findByClienteIdAndFechaBetweenAndFacturaIsNull(request.getClienteId(), request.getDesde(), request.getHasta());

        if (servicios.isEmpty()) {
            throw new RuntimeException("No hay servicios sin facturar para ese cliente en ese rango de fechas");
        }

        BigDecimal total = servicios.stream()
                .map(this::calcularCoste)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Factura factura = Factura.builder()
                .cliente(cliente)
                .numero(generarNumero())
                .importe(total)
                .fechaEmision(request.getFechaEmision())
                .build();

        facturaRepository.save(factura);

        for (Servicio s : servicios) {
            s.setFactura(factura);
            servicioRepository.save(s);
        }

        return factura;
    }

    private BigDecimal calcularCoste(Servicio servicio) {
        Tarifa tarifa = servicio.getTarifa();
        if (tarifa.getPrecioFijo() != null) {
            return tarifa.getPrecioFijo();
        }
        double duracionHoras = (servicio.getHoraFin().toSecondOfDay() - servicio.getHoraInicio().toSecondOfDay()) / 3600.0;
        return tarifa.getPrecioHora().multiply(BigDecimal.valueOf(duracionHoras));
    }

    private String generarNumero() {
        String anio = String.valueOf(Year.now().getValue());
        long siguiente = facturaRepository.contarPorAnio(anio) + 1;
        return String.format("FAC-%s-%03d", anio, siguiente);
    }
}