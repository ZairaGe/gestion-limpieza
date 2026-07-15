package com.mokeal.gestion.service;

import com.mokeal.gestion.dto.FacturaRequest;
import com.mokeal.gestion.dto.FacturaResponse;
import com.mokeal.gestion.model.Factura;
import com.mokeal.gestion.model.Servicio;
import com.mokeal.gestion.repository.FacturaRepository;
import com.mokeal.gestion.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final ServicioRepository servicioRepository;

    public FacturaService(FacturaRepository facturaRepository, ServicioRepository servicioRepository) {
        this.facturaRepository = facturaRepository;
        this.servicioRepository = servicioRepository;
    }

    public List<FacturaResponse> listarTodas() {
        return facturaRepository.findAll().stream()
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

    public FacturaResponse crear(FacturaRequest request) {
        if (facturaRepository.findByServicio_Id(request.getServicioId()).isPresent()) {
            throw new RuntimeException("Este servicio ya tiene una factura asociada");
        }

        Servicio servicio = servicioRepository.findById(request.getServicioId())
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con id: " + request.getServicioId()));

        Factura factura = Factura.builder()
                .servicio(servicio)
                .numero(request.getNumero())
                .importe(request.getImporte())
                .fechaEmision(request.getFechaEmision())
                .build();

        return convertir(facturaRepository.save(factura));
    }

    public FacturaResponse marcarComoPagada(Long id) {
        Factura factura = buscarEntidad(id);
        factura.setEstado(com.mokeal.gestion.model.EstadoFactura.PAGADA);
        return convertir(facturaRepository.save(factura));
    }

    public void eliminar(Long id) {
        Factura factura = buscarEntidad(id);
        facturaRepository.delete(factura);
    }

    private FacturaResponse convertir(Factura factura) {
        return FacturaResponse.builder()
                .id(factura.getId())
                .servicioId(factura.getServicio().getId())
                .numero(factura.getNumero())
                .importe(factura.getImporte())
                .estado(factura.getEstado())
                .fechaEmision(factura.getFechaEmision())
                .build();
    }
}