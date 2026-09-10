package com.mokeal.gestion.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.mokeal.gestion.model.Factura;
import com.mokeal.gestion.model.Servicio;
import com.mokeal.gestion.model.Tarifa;
import com.mokeal.gestion.repository.FacturaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FacturaPdfService {

    private final FacturaRepository facturaRepository;

    @Value("${mokeal.empresa.nombre:Mokeal}")
    private String empresaNombre;
    @Value("${mokeal.empresa.nif:}")
    private String empresaNif;
    @Value("${mokeal.empresa.direccion:}")
    private String empresaDireccion;
    @Value("${mokeal.empresa.telefono:}")
    private String empresaTelefono;
    @Value("${mokeal.empresa.email:}")
    private String empresaEmail;

    private static final DeviceRgb TEAL = new DeviceRgb(20, 184, 166);
    private static final DeviceRgb TEAL_OSCURO = new DeviceRgb(15, 61, 58);
    private static final DeviceRgb GRIS = new DeviceRgb(107, 114, 128);

    public FacturaPdfService(FacturaRepository facturaRepository) {
        this.facturaRepository = facturaRepository;
    }

    public byte[] generarPdf(Long facturaId) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con id: " + facturaId));

        List<Servicio> servicios = factura.getServicios().stream()
                .sorted((a, b) -> a.getFecha().compareTo(b.getFecha()))
                .collect(Collectors.toList());

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(salida))) {
            Document documento = new Document(pdfDoc);
            documento.setMargins(36, 36, 36, 36);

            documento.add(construirCabecera(factura));
            documento.add(construirDatosClienteYEmpresa(factura));

            Table tablaServicios = new Table(UnitValue.createPercentArray(new float[]{18, 22, 40, 20}))
                    .useAllAvailableWidth().setMarginTop(20);

            tablaServicios.addHeaderCell(celdaCabecera("Fecha"));
            tablaServicios.addHeaderCell(celdaCabecera("Horario"));
            tablaServicios.addHeaderCell(celdaCabecera("Dirección"));
            tablaServicios.addHeaderCell(celdaCabeceraDerecha("Importe"));

            for (Servicio s : servicios) {
                BigDecimal coste = calcularCoste(s);
                tablaServicios.addCell(celdaSimple(s.getFecha().toString()));
                tablaServicios.addCell(celdaSimple(s.getHoraInicio() + " - " + s.getHoraFin()));
                tablaServicios.addCell(celdaSimple(s.getDireccion()));
                tablaServicios.addCell(celdaDerecha(String.format("%.2f €", coste)));
            }

            documento.add(tablaServicios);
            documento.add(construirTotales(factura));
            documento.add(construirPie(factura));

            documento.close();
        }

        return salida.toByteArray();
    }

    private Table construirCabecera(Factura factura) {
        Table cabecera = new Table(UnitValue.createPercentArray(new float[]{15, 45, 40}))
                .useAllAvailableWidth();

        Table logoBox = new Table(1).setWidth(UnitValue.createPointValue(40));
        logoBox.addCell(new Cell()
                .add(new Paragraph("M").setFontColor(ColorConstants.WHITE).setBold().setFontSize(20)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(TEAL)
                .setBorder(Border.NO_BORDER)
                .setHeight(40)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE));

        Cell celdaLogo = new Cell().add(logoBox).setBorder(Border.NO_BORDER);
        Cell celdaNombre = new Cell()
                .add(new Paragraph(empresaNombre).setFontSize(20).setBold().setFontColor(TEAL_OSCURO))
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);

        Cell celdaFactura = new Cell()
                .add(new Paragraph("FACTURA").setFontSize(22).setBold().setFontColor(TEAL_OSCURO)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph(factura.getNumero()).setFontSize(12).setFontColor(GRIS)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);

        cabecera.addCell(celdaLogo);
        cabecera.addCell(celdaNombre);
        cabecera.addCell(celdaFactura);

        return cabecera;
    }

    private Table construirDatosClienteYEmpresa(Factura factura) {
        Table contenedor = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth().setMarginTop(25);

        Cell celdaCliente = new Cell().setBorder(Border.NO_BORDER);
        celdaCliente.add(new Paragraph("DATOS DEL CLIENTE").setBold().setFontSize(9).setFontColor(GRIS));
        celdaCliente.add(new Paragraph(factura.getCliente().getNombre()).setFontSize(11).setBold());
        if (factura.getCliente().getDireccion() != null) {
            celdaCliente.add(new Paragraph(factura.getCliente().getDireccion()).setFontSize(10).setFontColor(GRIS));
        }
        if (factura.getCliente().getTelefono() != null) {
            celdaCliente.add(new Paragraph(factura.getCliente().getTelefono()).setFontSize(10).setFontColor(GRIS));
        }

        Cell celdaEmpresa = new Cell().setBorder(Border.NO_BORDER);
        celdaEmpresa.add(new Paragraph("FECHA DE EMISIÓN").setBold().setFontSize(9).setFontColor(GRIS)
                .setTextAlignment(TextAlignment.RIGHT));
        celdaEmpresa.add(new Paragraph(factura.getFechaEmision().toString()).setFontSize(11).setBold()
                .setTextAlignment(TextAlignment.RIGHT));
        if (!empresaNif.isBlank()) {
            celdaEmpresa.add(new Paragraph("NIF: " + empresaNif).setFontSize(10).setFontColor(GRIS)
                    .setTextAlignment(TextAlignment.RIGHT));
        }
        if (!empresaTelefono.isBlank() || !empresaEmail.isBlank()) {
            celdaEmpresa.add(new Paragraph((empresaTelefono + "  " + empresaEmail).trim())
                    .setFontSize(10).setFontColor(GRIS).setTextAlignment(TextAlignment.RIGHT));
        }

        contenedor.addCell(celdaCliente);
        contenedor.addCell(celdaEmpresa);
        return contenedor;
    }

    private Table construirTotales(Factura factura) {
        Table contenedor = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth().setMarginTop(15);

        Table totales = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();

        BigDecimal subtotal = factura.getSubtotal() != null ? factura.getSubtotal() : factura.getImporte();
        BigDecimal descuentoPct = factura.getDescuentoPorcentaje() != null ? factura.getDescuentoPorcentaje() : BigDecimal.ZERO;
        BigDecimal iva = factura.getIvaImporte();

        filaTotales(totales, "Subtotal", String.format("%.2f €", subtotal), false);

        if (descuentoPct.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal importeDescuento = subtotal.multiply(descuentoPct).divide(BigDecimal.valueOf(100));
            filaTotales(totales, "Descuento (" + descuentoPct + "%)", "-" + String.format("%.2f €", importeDescuento), false);
        }

        if (iva != null) {
            filaTotales(totales, "IVA (21%)", String.format("%.2f €", iva), false);
        }

        filaTotales(totales, "TOTAL", String.format("%.2f €", factura.getImporte()), true);

        Cell vacio = new Cell().setBorder(Border.NO_BORDER);
        Cell celdaTotales = new Cell().add(totales).setBorder(Border.NO_BORDER);

        contenedor.addCell(vacio);
        contenedor.addCell(celdaTotales);
        return contenedor;
    }

    private void filaTotales(Table tabla, String etiqueta, String valor, boolean destacado) {
        Cell celdaEtiqueta = new Cell().add(new Paragraph(etiqueta)
                        .setFontSize(destacado ? 12 : 10)
                        .setBold(destacado)
                        .setFontColor(destacado ? TEAL_OSCURO : GRIS))
                .setBorder(destacado ? Border.NO_BORDER : Border.NO_BORDER)
                .setBorderTop(destacado ? new com.itextpdf.layout.borders.SolidBorder(TEAL, 1) : Border.NO_BORDER)
                .setPaddingTop(destacado ? 8 : 4);

        Cell celdaValor = new Cell().add(new Paragraph(valor)
                        .setFontSize(destacado ? 12 : 10)
                        .setBold(destacado)
                        .setFontColor(destacado ? TEAL_OSCURO : ColorConstants.BLACK)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER)
                .setBorderTop(destacado ? new com.itextpdf.layout.borders.SolidBorder(TEAL, 1) : Border.NO_BORDER)
                .setPaddingTop(destacado ? 8 : 4);

        tabla.addCell(celdaEtiqueta);
        tabla.addCell(celdaValor);
    }

    private Paragraph construirPie(Factura factura) {
        return new Paragraph("Estado: " + factura.getEstado())
                .setFontSize(9).setFontColor(GRIS).setMarginTop(30);
    }

    private Cell celdaCabecera(String texto) {
        return new Cell().add(new Paragraph(texto).setBold().setFontSize(9))
                .setBackgroundColor(TEAL)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(6);
    }

    private Cell celdaCabeceraDerecha(String texto) {
        return new Cell().add(new Paragraph(texto).setBold().setFontSize(9))
                .setBackgroundColor(TEAL)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(6);
    }

    private Cell celdaSimple(String texto) {
        return new Cell().add(new Paragraph(texto).setFontSize(10)).setPadding(6);
    }

    private Cell celdaDerecha(String texto) {
        return new Cell().add(new Paragraph(texto).setFontSize(10).setTextAlignment(TextAlignment.RIGHT)).setPadding(6);
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
}