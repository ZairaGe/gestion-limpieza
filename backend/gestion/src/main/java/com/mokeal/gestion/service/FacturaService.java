package com.mokeal.gestion.service;

import com.mokeal.gestion.dto.FacturaResponse;
import com.mokeal.gestion.model.EstadoFactura;
import com.mokeal.gestion.model.Factura;
import com.mokeal.gestion.model.Servicio;
import com.mokeal.gestion.repository.FacturaRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;

    public FacturaService(FacturaRepository facturaRepository) {
        this.facturaRepository = facturaRepository;
    }

    public List<FacturaResponse> listarTodas() {
        return facturaRepository.findAll().stream()
                .map(this::convertir)
                .collect(Collectors.toList());
    }

    public List<FacturaResponse> listarPorCliente(Long clienteId) {
        return facturaRepository.findByCliente_Id(clienteId).stream()
                .map(this::convertir)
                .collect(Collectors.toList());
    }

    public FacturaResponse buscarPorId(Long id) {
        return convertir(buscarEntidad(id));
    }

    private Factura buscarEntidad(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con id: " + id));
    }

    public FacturaResponse marcarComoPagada(Long id) {
        Factura factura = buscarEntidad(id);
        factura.setEstado(EstadoFactura.PAGADA);
        return convertir(facturaRepository.save(factura));
    }

    public void eliminar(Long id) {
        Factura factura = buscarEntidad(id);
        for (Servicio s : factura.getServicios()) {
            s.setFactura(null);
        }
        facturaRepository.delete(factura);
    }

    private FacturaResponse convertir(Factura factura) {
    return FacturaResponse.builder()
            .id(factura.getId())
            .clienteId(factura.getCliente().getId())
            .clienteNombre(factura.getCliente().getNombre())
            .numero(factura.getNumero())
            .concepto(factura.getConcepto())
            .importe(factura.getImporte())
            .estado(factura.getEstado())
            .fechaEmision(factura.getFechaEmision())
            .cantidadServicios(factura.getServicios().size())
            .subtotal(factura.getSubtotal())
            .descuentoPorcentaje(factura.getDescuentoPorcentaje())
            .ivaImporte(factura.getIvaImporte())
            .build();
}
}