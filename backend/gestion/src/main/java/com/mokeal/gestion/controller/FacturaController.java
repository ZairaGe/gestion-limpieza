package com.mokeal.gestion.controller;

import com.mokeal.gestion.dto.FacturaResponse;
import com.mokeal.gestion.service.FacturaService;
import com.mokeal.gestion.service.FacturaPdfService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService facturaService;

    private final FacturaPdfService facturaPdfService;

    public FacturaController(FacturaService facturaService, FacturaPdfService facturaPdfService) {
        this.facturaService = facturaService;
        this.facturaPdfService = facturaPdfService;
    }

    @GetMapping
    public List<FacturaResponse> listar(@RequestParam(required = false) Long clienteId) {
        if (clienteId != null) {
            return facturaService.listarPorCliente(clienteId);
        }
        return facturaService.listarTodas();
    }

    @GetMapping("/{id}")
    public FacturaResponse buscar(@PathVariable Long id) {
        return facturaService.buscarPorId(id);
    }

    @PatchMapping("/{id}/pagar")
    public FacturaResponse marcarComoPagada(@PathVariable Long id) {
        return facturaService.marcarComoPagada(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        facturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
    byte[] pdf = facturaPdfService.generarPdf(id);

    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
    headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
            .filename("factura.pdf").build());

    return ResponseEntity.ok().headers(headers).body(pdf);
}
}