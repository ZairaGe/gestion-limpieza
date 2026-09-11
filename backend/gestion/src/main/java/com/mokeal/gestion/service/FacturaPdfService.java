package com.mokeal.gestion.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
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
    private static final DeviceRgb TEAL_SUAVE = new DeviceRgb(240, 253, 250);
    private static final DeviceRgb GRIS = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb GRIS_CLARO = new DeviceRgb(229, 231, 235);
    private static final DeviceRgb GRIS_FILA = new DeviceRgb(249, 250, 251);

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
            Document documento = new Document(pdfDoc, PageSize.A4);
            documento.setMargins(0, 40, 40, 40);

            documento.add(construirBannerCabecera(factura));

            Div cuerpo = new Div().setMarginTop(30);
            cuerpo.add(construirDatosClienteYEmpresa(factura));
            cuerpo.add(construirTablaServicios(servicios));
            cuerpo.add(construirTotales(factura));
            cuerpo.add(construirPie());

            documento.add(cuerpo);
            documento.close();
        }

        return salida.toByteArray();
    }

    private Div construirBannerCabecera(Factura factura) {
        Div banner = new Div()
                .setBackgroundColor(TEAL_OSCURO)
                .setPaddingTop(30).setPaddingBottom(30).setPaddingLeft(40).setPaddingRight(40)
                .setWidth(UnitValue.createPercentValue(100));

        Table fila = new Table(UnitValue.createPercentArray(new float[]{12, 43, 45})).useAllAvailableWidth();

        Cell celdaLogo = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        Table logoBox = new Table(1).setWidth(UnitValue.createPointValue(38));
        logoBox.addCell(new Cell()
                .add(new Paragraph("✦").setFontColor(TEAL_OSCURO).setBold().setFontSize(18)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(Border.NO_BORDER)
                .setHeight(38)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));
        celdaLogo.add(logoBox);

        Cell celdaNombre = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(empresaNombre).setFontColor(ColorConstants.WHITE).setBold().setFontSize(20));

        Cell celdaFactura = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph("FACTURA").setFontColor(ColorConstants.WHITE).setBold().setFontSize(20)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph(factura.getNumero()).setFontColor(TEAL).setFontSize(11)
                        .setTextAlignment(TextAlignment.RIGHT));

        fila.addCell(celdaLogo);
        fila.addCell(celdaNombre);
        fila.addCell(celdaFactura);

        banner.add(fila);
        return banner;
    }

    private Table construirDatosClienteYEmpresa(Factura factura) {
        Table contenedor = new Table(UnitValue.createPercentArray(new float[]{55, 45})).useAllAvailableWidth();

        Div boxCliente = new Div()
                .setBackgroundColor(TEAL_SUAVE)
                .setPadding(14);
        boxCliente.add(new Paragraph("FACTURAR A").setBold().setFontSize(8).setFontColor(TEAL_OSCURO).setMarginBottom(4));
        boxCliente.add(new Paragraph(factura.getCliente().getNombre()).setFontSize(12).setBold());
        if (factura.getCliente().getDireccion() != null && !factura.getCliente().getDireccion().isBlank()) {
            boxCliente.add(new Paragraph(factura.getCliente().getDireccion()).setFontSize(9).setFontColor(GRIS));
        }
        if (factura.getCliente().getTelefono() != null && !factura.getCliente().getTelefono().isBlank()) {
            boxCliente.add(new Paragraph(factura.getCliente().getTelefono()).setFontSize(9).setFontColor(GRIS));
        }
        if (factura.getCliente().getEmail() != null && !factura.getCliente().getEmail().isBlank()) {
            boxCliente.add(new Paragraph(factura.getCliente().getEmail()).setFontSize(9).setFontColor(GRIS));
        }

        Div boxDatos = new Div().setPadding(14);
        filaDato(boxDatos, "Fecha de emisión", factura.getFechaEmision().toString());
        if (!empresaNif.isBlank()) filaDato(boxDatos, "NIF", empresaNif);
        if (!empresaTelefono.isBlank()) filaDato(boxDatos, "Teléfono", empresaTelefono);
        if (!empresaEmail.isBlank()) filaDato(boxDatos, "Email", empresaEmail);
        if (!empresaDireccion.isBlank()) filaDato(boxDatos, "Dirección", empresaDireccion);

        contenedor.addCell(new Cell().add(boxCliente).setBorder(Border.NO_BORDER).setPadding(0).setPaddingRight(8));
        contenedor.addCell(new Cell().add(boxDatos).setBorder(Border.NO_BORDER).setPadding(0).setPaddingLeft(8));
        return contenedor;
    }

    private void filaDato(Div contenedor, String etiqueta, String valor) {
        Table fila = new Table(UnitValue.createPercentArray(new float[]{45, 55})).useAllAvailableWidth();
        fila.addCell(new Cell().setBorder(Border.NO_BORDER).setPaddingBottom(3)
                .add(new Paragraph(etiqueta).setFontSize(9).setFontColor(GRIS)));
        fila.addCell(new Cell().setBorder(Border.NO_BORDER).setPaddingBottom(3)
                .add(new Paragraph(valor).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)));
        contenedor.add(fila);
    }

    private Table construirTablaServicios(List<Servicio> servicios) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{18, 15, 47, 20}))
                .useAllAvailableWidth().setMarginTop(25);

        tabla.addHeaderCell(celdaCabecera("Fecha"));
        tabla.addHeaderCell(celdaCabecera("Duración"));
        tabla.addHeaderCell(celdaCabecera("Dirección"));
        tabla.addHeaderCell(celdaCabeceraDerecha("Importe"));

        boolean alterna = false;
        for (Servicio s : servicios) {
            BigDecimal coste = calcularCoste(s);
            double duracion = obtenerDuracion(s);
            DeviceRgb colorFila = alterna ? GRIS_FILA : new DeviceRgb(255, 255, 255);

            tabla.addCell(celdaFila(s.getFecha().toString(), colorFila, TextAlignment.LEFT));
            tabla.addCell(celdaFila(String.format("%.2fh", duracion), colorFila, TextAlignment.LEFT));
            tabla.addCell(celdaFila(s.getDireccion(), colorFila, TextAlignment.LEFT));
            tabla.addCell(celdaFila(String.format("%.2f €", coste), colorFila, TextAlignment.RIGHT));
            alterna = !alterna;
        }

        return tabla;
    }

    private Table construirTotales(Factura factura) {
        Table contenedor = new Table(UnitValue.createPercentArray(new float[]{55, 45})).useAllAvailableWidth().setMarginTop(15);

        BigDecimal subtotal = factura.getSubtotal() != null ? factura.getSubtotal() : factura.getImporte();
        BigDecimal descuentoPct = factura.getDescuentoPorcentaje() != null ? factura.getDescuentoPorcentaje() : BigDecimal.ZERO;
        BigDecimal iva = factura.getIvaImporte();

        Div cajaTotales = new Div()
                .setBackgroundColor(TEAL_SUAVE)
                .setPadding(14);

        filaTotales(cajaTotales, "Subtotal", String.format("%.2f €", subtotal), false);

        if (descuentoPct.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal importeDescuento = subtotal.multiply(descuentoPct).divide(BigDecimal.valueOf(100));
            filaTotales(cajaTotales, "Descuento (" + descuentoPct.stripTrailingZeros().toPlainString() + "%)",
                    "-" + String.format("%.2f €", importeDescuento), false);
        }

        if (iva != null) {
            filaTotales(cajaTotales, "IVA (21%)", String.format("%.2f €", iva), false);
        }

        cajaTotales.add(new com.itextpdf.layout.element.LineSeparator(
                new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1))
                .setStrokeColor(TEAL).setMarginTop(6).setMarginBottom(6));

        filaTotales(cajaTotales, "TOTAL", String.format("%.2f €", factura.getImporte()), true);

        contenedor.addCell(new Cell().setBorder(Border.NO_BORDER));
        contenedor.addCell(new Cell().add(cajaTotales).setBorder(Border.NO_BORDER).setPadding(0));
        return contenedor;
    }

    private void filaTotales(Div contenedor, String etiqueta, String valor, boolean destacado) {
        Table fila = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();

        Paragraph pEtiqueta = new Paragraph(etiqueta)
                .setFontSize(destacado ? 13 : 10)
                .setFontColor(destacado ? TEAL_OSCURO : GRIS);
        Paragraph pValor = new Paragraph(valor)
                .setFontSize(destacado ? 13 : 10)
                .setFontColor(destacado ? TEAL_OSCURO : ColorConstants.BLACK)
                .setTextAlignment(TextAlignment.RIGHT);
        if (destacado) {
            pEtiqueta.setBold();
            pValor.setBold();
        }

        fila.addCell(new Cell().setBorder(Border.NO_BORDER).add(pEtiqueta));
        fila.addCell(new Cell().setBorder(Border.NO_BORDER).add(pValor));
        contenedor.add(fila);
    }

    private Div construirPie() {
        Div pie = new Div().setMarginTop(35).setPaddingTop(12)
                .setBorderTop(new SolidBorder(GRIS_CLARO, 1));

        Paragraph linea1 = new Paragraph(empresaNombre + (empresaTelefono.isBlank() ? "" : " · " + empresaTelefono))
                .setFontSize(9).setFontColor(GRIS).setTextAlignment(TextAlignment.CENTER);
        pie.add(linea1);

        if (!empresaEmail.isBlank() || !empresaDireccion.isBlank()) {
            String segundaLinea = (empresaEmail.isBlank() ? "" : empresaEmail)
                    + (empresaDireccion.isBlank() ? "" : (empresaEmail.isBlank() ? "" : " · ") + empresaDireccion);
            pie.add(new Paragraph(segundaLinea).setFontSize(9).setFontColor(GRIS).setTextAlignment(TextAlignment.CENTER));
        }

        pie.add(new Paragraph("Gracias por confiar en nosotros")
                .setFontSize(9).setFontColor(TEAL).setItalic().setMarginTop(8).setTextAlignment(TextAlignment.CENTER));

        return pie;
    }

    private Cell celdaCabecera(String texto) {
        return new Cell().add(new Paragraph(texto).setBold().setFontSize(9))
                .setBackgroundColor(TEAL_OSCURO)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.LEFT)
                .setPadding(8)
                .setBorder(Border.NO_BORDER);
    }

    private Cell celdaCabeceraDerecha(String texto) {
        return new Cell().add(new Paragraph(texto).setBold().setFontSize(9))
                .setBackgroundColor(TEAL_OSCURO)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(8)
                .setBorder(Border.NO_BORDER);
    }

    private Cell celdaFila(String texto, DeviceRgb color, TextAlignment alineacion) {
        return new Cell().add(new Paragraph(texto).setFontSize(9.5f))
                .setBackgroundColor(color)
                .setTextAlignment(alineacion)
                .setPadding(8)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(GRIS_CLARO, 0.5f));
    }

    private double obtenerDuracion(Servicio servicio) {
        return servicio.getDuracionHoras() != null
                ? servicio.getDuracionHoras()
                : (servicio.getHoraFin().toSecondOfDay() - servicio.getHoraInicio().toSecondOfDay()) / 3600.0;
    }

    private BigDecimal calcularCoste(Servicio servicio) {
        Tarifa tarifa = servicio.getTarifa();
        if (tarifa.getPrecioFijo() != null) {
            return tarifa.getPrecioFijo();
        }
        return tarifa.getPrecioHora().multiply(BigDecimal.valueOf(obtenerDuracion(servicio)));
    }
}