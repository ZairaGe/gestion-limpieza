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

        List<Servicio> servicios = servicioRepository.findAllById(request.getServicioIds());

        if (servicios.isEmpty()) {
            throw new RuntimeException("No se encontraron los servicios seleccionados");
        }

        for (Servicio s : servicios) {
            if (s.getFactura() != null) {
                throw new RuntimeException("El servicio del " + s.getFecha() + " ya está facturado");
            }
            if (!s.getCliente().getId().equals(request.getClienteId())) {
                throw new RuntimeException("Todos los servicios deben ser del mismo cliente");
            }
        }

        BigDecimal subtotal = servicios.stream()
                .map(this::calcularCoste)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal descuentoPorcentaje = request.getDescuentoPorcentaje() != null
                ? BigDecimal.valueOf(request.getDescuentoPorcentaje())
                : BigDecimal.ZERO;

        BigDecimal importeDescuento = subtotal.multiply(descuentoPorcentaje)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        BigDecimal baseImponible = subtotal.subtract(importeDescuento);

        BigDecimal iva = baseImponible.multiply(BigDecimal.valueOf(0.21))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal total = baseImponible.add(iva).setScale(2, java.math.RoundingMode.HALF_UP);

        String numeroFinal = (request.getNumero() != null && !request.getNumero().isBlank())
                ? request.getNumero()
                : generarNumero();

        Factura factura = Factura.builder()
                .cliente(cliente)
                .numero(numeroFinal)
                .subtotal(subtotal)
                .descuentoPorcentaje(descuentoPorcentaje)
                .ivaImporte(iva)
                .importe(total)
                .fechaEmision(request.getFechaEmision())
                .concepto(request.getConcepto() != null && !request.getConcepto().isBlank() ? request.getConcepto()
                        : "Servicios de limpieza")
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
        double duracion = servicio.getDuracionHoras() != null
                ? servicio.getDuracionHoras()
                : (servicio.getHoraFin().toSecondOfDay() - servicio.getHoraInicio().toSecondOfDay()) / 3600.0;
        return tarifa.getPrecioHora().multiply(BigDecimal.valueOf(duracion));
    }

    private String generarNumero() {
        String anio = String.valueOf(Year.now().getValue());
        long siguiente = facturaRepository.contarPorAnio(anio) + 1;
        return String.format("FAC-%s-%03d", anio, siguiente);
    }
}