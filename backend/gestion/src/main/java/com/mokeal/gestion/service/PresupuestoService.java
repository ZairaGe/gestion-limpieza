package com.mokeal.gestion.service;

import com.mokeal.gestion.dto.LineaPresupuestoRequest;
import com.mokeal.gestion.dto.PresupuestoRequest;
import com.mokeal.gestion.dto.PresupuestoResponse;
import com.mokeal.gestion.model.*;
import com.mokeal.gestion.repository.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PresupuestoService {

    private final PresupuestoRepository presupuestoRepository;
    private final ClienteRepository clienteRepository;

    public PresupuestoService(PresupuestoRepository presupuestoRepository, ClienteRepository clienteRepository) {
        this.presupuestoRepository = presupuestoRepository;
        this.clienteRepository = clienteRepository;
    }

    public List<PresupuestoResponse> listarTodos() {
        return presupuestoRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")
        ).stream().map(this::convertir).collect(Collectors.toList());
    }

    public List<PresupuestoResponse> listarPorCliente(Long clienteId) {
        return presupuestoRepository.findByCliente_Id(clienteId).stream()
                .map(this::convertir).collect(Collectors.toList());
    }

    public PresupuestoResponse buscarPorId(Long id) {
        return convertir(buscarEntidad(id));
    }

    private Presupuesto buscarEntidad(Long id) {
        return presupuestoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Presupuesto no encontrado con id: " + id));
    }

    public PresupuestoResponse crear(PresupuestoRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        List<LineaPresupuesto> lineas = new ArrayList<>();
        for (LineaPresupuestoRequest lr : request.getLineas()) {
            BigDecimal subtotalLinea = lr.getPrecioHora()
                    .multiply(BigDecimal.valueOf(lr.getHoras()))
                    .setScale(2, RoundingMode.HALF_UP);

            lineas.add(LineaPresupuesto.builder()
                    .concepto(lr.getConcepto())
                    .horas(lr.getHoras())
                    .precioHora(lr.getPrecioHora())
                    .subtotal(subtotalLinea)
                    .build());
        }

        BigDecimal subtotal = lineas.stream()
                .map(LineaPresupuesto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal descuentoPorcentaje = request.getDescuentoPorcentaje() != null
                ? request.getDescuentoPorcentaje() : BigDecimal.ZERO;

        BigDecimal importeDescuento = subtotal.multiply(descuentoPorcentaje)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal total = subtotal.subtract(importeDescuento).setScale(2, RoundingMode.HALF_UP);

        Presupuesto presupuesto = Presupuesto.builder()
                .cliente(cliente)
                .numero(generarNumero())
                .lineas(lineas)
                .subtotal(subtotal)
                .descuentoPorcentaje(descuentoPorcentaje)
                .total(total)
                .fechaEmision(request.getFechaEmision())
                .build();

        return convertir(presupuestoRepository.save(presupuesto));
    }

    public PresupuestoResponse cambiarEstado(Long id, EstadoPresupuesto nuevoEstado) {
        Presupuesto p = buscarEntidad(id);
        p.setEstado(nuevoEstado);
        return convertir(presupuestoRepository.save(p));
    }

    public void eliminar(Long id) {
        presupuestoRepository.delete(buscarEntidad(id));
    }

    private String generarNumero() {
        String anio = String.valueOf(Year.now().getValue());
        long siguiente = presupuestoRepository.contarPorAnio(anio) + 1;
        return String.format("PRE-%s-%03d", anio, siguiente);
    }

    private PresupuestoResponse convertir(Presupuesto p) {
        List<PresupuestoResponse.LineaResponse> lineasResponse = p.getLineas().stream()
                .map(l -> PresupuestoResponse.LineaResponse.builder()
                        .concepto(l.getConcepto())
                        .horas(l.getHoras())
                        .precioHora(l.getPrecioHora())
                        .subtotal(l.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return PresupuestoResponse.builder()
                .id(p.getId())
                .clienteId(p.getCliente().getId())
                .clienteNombre(p.getCliente().getNombre())
                .numero(p.getNumero())
                .lineas(lineasResponse)
                .subtotal(p.getSubtotal())
                .descuentoPorcentaje(p.getDescuentoPorcentaje())
                .total(p.getTotal())
                .estado(p.getEstado())
                .fechaEmision(p.getFechaEmision())
                .build();
    }
}