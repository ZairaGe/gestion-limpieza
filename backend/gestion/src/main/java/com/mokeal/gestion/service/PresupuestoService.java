package com.mokeal.gestion.service;

import com.mokeal.gestion.dto.LineaPresupuestoRequest;
import com.mokeal.gestion.dto.PresupuestoRequest;
import com.mokeal.gestion.dto.PresupuestoResponse;
import com.mokeal.gestion.model.LineaPresupuesto;
import com.mokeal.gestion.model.Presupuesto;
import com.mokeal.gestion.repository.PresupuestoRepository;
import org.springframework.data.domain.Sort;
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

        public PresupuestoService(PresupuestoRepository presupuestoRepository) {
                this.presupuestoRepository = presupuestoRepository;
        }

        public List<PresupuestoResponse> listarTodos() {
                return presupuestoRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                                .map(this::convertir)
                                .collect(Collectors.toList());
        }

        public PresupuestoResponse buscarPorId(Long id) {
                return convertir(buscarEntidad(id));
        }

        private Presupuesto buscarEntidad(Long id) {
                return presupuestoRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Presupuesto no encontrado con id: " + id));
        }

        public PresupuestoResponse crear(PresupuestoRequest request) {
                List<LineaPresupuesto> lineas = new ArrayList<>();
                for (LineaPresupuestoRequest lr : request.getLineas()) {
                        BigDecimal subtotalLinea = lr.getPrecioHora()
                                        .multiply(BigDecimal.valueOf(lr.getHoras()))
                                        .setScale(2, RoundingMode.HALF_UP);

                        lineas.add(LineaPresupuesto.builder()
                                        .concepto(lr.getConcepto().trim())
                                        .horas(lr.getHoras())
                                        .precioHora(lr.getPrecioHora())
                                        .subtotal(subtotalLinea)
                                        .build());
                }

                BigDecimal subtotal = lineas.stream()
                                .map(LineaPresupuesto::getSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP);

                BigDecimal descuentoPct = request.getDescuentoPorcentaje() != null
                                ? request.getDescuentoPorcentaje()
                                : BigDecimal.ZERO;
                BigDecimal ivaPct = request.getIvaPorcentaje() != null
                                ? request.getIvaPorcentaje()
                                : BigDecimal.valueOf(21);

                BigDecimal importeDescuento = subtotal.multiply(descuentoPct)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal baseImponible = subtotal.subtract(importeDescuento);
                BigDecimal ivaImporte = baseImponible.multiply(ivaPct)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal total = baseImponible.add(ivaImporte).setScale(2, RoundingMode.HALF_UP);

                Presupuesto presupuesto = Presupuesto.builder()
                                .nombre(request.getNombre().trim())
                                .telefono(limpiar(request.getTelefono()))
                                .correo(limpiar(request.getCorreo()))
                                .direccion(limpiar(request.getDireccion()))
                                .numero(generarNumero())
                                .lineas(lineas)
                                .notas(limpiar(request.getNotas()))
                                .subtotal(subtotal)
                                .descuentoPorcentaje(descuentoPct)
                                .ivaPorcentaje(ivaPct)
                                .ivaImporte(ivaImporte)
                                .total(total)
                                .fechaEmision(request.getFechaEmision())
                                .build();

                return convertir(presupuestoRepository.save(presupuesto));
        }

        public PresupuestoResponse actualizar(Long id, PresupuestoRequest request) {
                Presupuesto presupuesto = buscarEntidad(id);

                // Actualizar los datos del cliente
                presupuesto.setNombre(request.getNombre().trim());
                presupuesto.setTelefono(limpiar(request.getTelefono()));
                presupuesto.setCorreo(limpiar(request.getCorreo()));
                presupuesto.setDireccion(limpiar(request.getDireccion()));
                presupuesto.setNotas(limpiar(request.getNotas()));
                presupuesto.setFechaEmision(request.getFechaEmision());

                // Preparar las nuevas líneas
                List<LineaPresupuesto> lineas = new ArrayList<>();

                for (LineaPresupuestoRequest lr : request.getLineas()) {
                        BigDecimal subtotalLinea = lr.getPrecioHora()
                                        .multiply(BigDecimal.valueOf(lr.getHoras()))
                                        .setScale(2, RoundingMode.HALF_UP);

                        lineas.add(LineaPresupuesto.builder()
                                        .concepto(lr.getConcepto().trim())
                                        .horas(lr.getHoras())
                                        .precioHora(lr.getPrecioHora())
                                        .subtotal(subtotalLinea)
                                        .build());
                }

                // Sustituir las líneas anteriores
                presupuesto.getLineas().clear();
                presupuesto.getLineas().addAll(lineas);

                // Recalcular los importes
                BigDecimal subtotal = lineas.stream()
                                .map(LineaPresupuesto::getSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP);

                BigDecimal descuentoPct = request.getDescuentoPorcentaje() != null
                                ? request.getDescuentoPorcentaje()
                                : BigDecimal.ZERO;

                BigDecimal ivaPct = request.getIvaPorcentaje() != null
                                ? request.getIvaPorcentaje()
                                : BigDecimal.valueOf(21);

                BigDecimal importeDescuento = subtotal.multiply(descuentoPct)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                BigDecimal baseImponible = subtotal.subtract(importeDescuento);

                BigDecimal ivaImporte = baseImponible.multiply(ivaPct)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                BigDecimal total = baseImponible.add(ivaImporte)
                                .setScale(2, RoundingMode.HALF_UP);

                presupuesto.setSubtotal(subtotal);
                presupuesto.setDescuentoPorcentaje(descuentoPct);
                presupuesto.setIvaPorcentaje(ivaPct);
                presupuesto.setIvaImporte(ivaImporte);
                presupuesto.setTotal(total);

                // Guardar conservando el ID y el número originales
                return convertir(presupuestoRepository.save(presupuesto));
        }

        public void eliminar(Long id) {
                presupuestoRepository.delete(buscarEntidad(id));
        }

        private String limpiar(String texto) {
                return texto != null && !texto.isBlank() ? texto.trim() : null;
        }

        private String generarNumero() {
                String anio = String.valueOf(Year.now().getValue());
                long siguiente = presupuestoRepository.contarPorAnio(anio) + 1;
                return String.format("PRE-%s-%03d", anio, siguiente);
        }

        private PresupuestoResponse convertir(Presupuesto p) {
                List<PresupuestoResponse.LineaResponse> lineas = p.getLineas().stream()
                                .map(l -> PresupuestoResponse.LineaResponse.builder()
                                                .concepto(l.getConcepto())
                                                .horas(l.getHoras())
                                                .precioHora(l.getPrecioHora())
                                                .subtotal(l.getSubtotal())
                                                .build())
                                .collect(Collectors.toList());

                return PresupuestoResponse.builder()
                                .id(p.getId())
                                .nombre(p.getNombre())
                                .telefono(p.getTelefono())
                                .correo(p.getCorreo())
                                .direccion(p.getDireccion())
                                .numero(p.getNumero())
                                .lineas(lineas)
                                .notas(p.getNotas())
                                .subtotal(p.getSubtotal())
                                .descuentoPorcentaje(p.getDescuentoPorcentaje() != null ? p.getDescuentoPorcentaje()
                                                : BigDecimal.ZERO)
                                .ivaPorcentaje(p.getIvaPorcentaje() != null ? p.getIvaPorcentaje() : BigDecimal.ZERO)
                                .ivaImporte(p.getIvaImporte() != null ? p.getIvaImporte() : BigDecimal.ZERO)
                                .total(p.getTotal())
                                .fechaEmision(p.getFechaEmision())
                                .build();
        }
}