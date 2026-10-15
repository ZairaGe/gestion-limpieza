package com.mokeal.gestion.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

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
    @Value("${mokeal.empresa.cuentaBancaria:}")
    private String cuentaBancaria;
    @Value("${mokeal.empresa.banco:}")
    private String banco;

    private static final DeviceRgb CREMA_FONDO = new DeviceRgb(252, 250, 246);
    private static final DeviceRgb CREMA_OSCURO = new DeviceRgb(224, 211, 185);
    private static final DeviceRgb CREMA_CLARO = new DeviceRgb(240, 232, 218);
    private static final DeviceRgb GRIS_TITULO = new DeviceRgb(60, 60, 60);
    private static final DeviceRgb GRIS = new DeviceRgb(110, 105, 95);
    private static final DeviceRgb NEGRO = new DeviceRgb(30, 30, 30);
    private static final DeviceRgb BORDE = new DeviceRgb(214, 205, 188);

    public FacturaPdfService(FacturaRepository facturaRepository) {
        this.facturaRepository = facturaRepository;
    }

    public byte[] generarPdf(Long facturaId) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con id: " + facturaId));

        List<Servicio> servicios = factura.getServicios().stream()
                .sorted((a, b) -> a.getFecha().compareTo(b.getFecha()))
                .toList();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(salida))) {
            Document documento = new Document(pdfDoc, PageSize.A4);
            documento.setMargins(45, 45, 45, 45);
            documento.setBackgroundColor(CREMA_FONDO);

            documento.add(construirCabecera(factura));
            documento.add(construirCajaCliente(factura));
            documento.add(construirTablaConcepto(servicios));
            documento.add(construirFilaInferior(factura));
            documento.add(construirPie());

            documento.close();
        }

        return salida.toByteArray();
    }

    private Image cargarLogo() {
        try {
            ClassPathResource recurso = new ClassPathResource("static/logofactura.png");
            ImageData imageData = ImageDataFactory.create(recurso.getInputStream().readAllBytes());
            Image logo = new Image(imageData);
            logo.setWidth(85);
            logo.setAutoScaleHeight(true);
            return logo;
        } catch (Exception e) {
            System.err.println("No se pudo cargar el logo: " + e.getMessage());
            return null;
        }
    }

    private Table construirCabecera(Factura factura) {
        Table fila = new Table(UnitValue.createPercentArray(new float[] { 35, 65 })).useAllAvailableWidth();

        Cell celdaLogo = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.TOP);
        Image logo = cargarLogo();
        if (logo != null) {
            celdaLogo.add(logo);
        } else {
            celdaLogo.add(new Paragraph(empresaNombre).setBold().setFontSize(16));
        }
        celdaLogo.add(new Paragraph(empresaNombre).setFontSize(9).setFontColor(GRIS).setMarginTop(6));
        if (!empresaNif.isBlank())
            celdaLogo.add(new Paragraph(empresaNif).setFontSize(8).setFontColor(GRIS));
        if (!empresaDireccion.isBlank())
            celdaLogo.add(new Paragraph(empresaDireccion).setFontSize(8).setFontColor(GRIS));
        if (!empresaEmail.isBlank())
            celdaLogo.add(new Paragraph(empresaEmail).setFontSize(8).setFontColor(GRIS));
        if (!empresaTelefono.isBlank())
            celdaLogo.add(new Paragraph(empresaTelefono).setFontSize(8).setFontColor(GRIS));

        Cell celdaDerecha = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        celdaDerecha.add(new Paragraph("FACTURA").setBold().setFontSize(30).setFontColor(GRIS_TITULO));
        celdaDerecha.add(new Paragraph(factura.getNumero()).setBold().setFontSize(14)
                .setFontColor(NEGRO).setMarginTop(4));

        Table filaInfo = new Table(UnitValue.createPercentArray(new float[] { 55, 45 }))
                .setWidth(UnitValue.createPercentValue(70))
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.RIGHT)
                .setMarginTop(10);
        filaInfo.addCell(infoCelda("Fecha de emisión:", false));
        filaInfo.addCell(infoCelda(factura.getFechaEmision().toString(), true));
        filaInfo.addCell(infoCelda("N.º de factura:", false));
        filaInfo.addCell(infoCelda(factura.getNumero(), true));
        celdaDerecha.add(filaInfo);

        fila.addCell(celdaLogo);
        fila.addCell(celdaDerecha);
        return fila;
    }

    private Cell infoCelda(String texto, boolean negrita) {
        Paragraph p = new Paragraph(texto).setFontSize(9).setTextAlignment(TextAlignment.RIGHT);
        if (negrita) {
            p.setBold();
        } else {
            p.setFontColor(GRIS);
        }
        return new Cell().add(p).setBorder(Border.NO_BORDER).setPaddingBottom(3);
    }

    private Div construirCajaCliente(Factura factura) {
        Div caja = new Div().setMarginTop(25);
        caja.add(new Paragraph("DATOS DEL CLIENTE").setBold().setFontSize(11).setFontColor(GRIS_TITULO)
                .setMarginBottom(6));
        caja.add(new Paragraph("Nombre: " + factura.getCliente().getNombre()).setFontSize(9.5f));
        if (factura.getCliente().getNif() != null && !factura.getCliente().getNif().isBlank()) {
            caja.add(new Paragraph("CIF/NIF: " + factura.getCliente().getNif()).setFontSize(9.5f));
        }
        if (factura.getCliente().getDireccion() != null && !factura.getCliente().getDireccion().isBlank()) {
            String direccionCompleta = factura.getCliente().getDireccion();
            if (factura.getCliente().getCodigoPostal() != null && !factura.getCliente().getCodigoPostal().isBlank()) {
                direccionCompleta += ", " + factura.getCliente().getCodigoPostal();
            }
            caja.add(new Paragraph("Dirección: " + direccionCompleta).setFontSize(9.5f));
        }
        return caja;
    }

    private Table construirTablaConcepto(List<Servicio> servicios) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[] { 42, 16, 21, 21 }))
                .useAllAvailableWidth().setMarginTop(22);

        tabla.addHeaderCell(celdaCabecera("PRODUCTO / SERVICIO", TextAlignment.LEFT));
        tabla.addHeaderCell(celdaCabecera("CANTIDAD", TextAlignment.CENTER));
        tabla.addHeaderCell(celdaCabecera("PRECIO/H", TextAlignment.CENTER));
        tabla.addHeaderCell(celdaCabecera("TOTAL", TextAlignment.RIGHT));

        boolean alterna = false;
        for (Servicio s : servicios) {
            BigDecimal coste = calcularCoste(s);
            double duracion = obtenerDuracion(s);
            String etiquetaServicio = etiquetaTipoServicio(s);

            tabla.addCell(celdaFila(etiquetaServicio, TextAlignment.LEFT, alterna));
            tabla.addCell(celdaFila(String.format("%.2f", duracion), TextAlignment.CENTER, alterna));
            tabla.addCell(celdaFila(String.format("%.2f €", coste.doubleValue() / Math.max(duracion, 0.01)),
                    TextAlignment.CENTER, alterna));
            tabla.addCell(celdaFila(String.format("%.2f €", coste), TextAlignment.RIGHT, alterna));
            alterna = !alterna;
        }

        return tabla;
    }

    private String etiquetaTipoServicio(Servicio s) {
        String tipo = s.getTarifa().getTipoServicio().name();
        if (s.getTarifa() == null)
            return "Servicio de limpieza";
        return switch (tipo) {
            case "CASA" -> "Limpieza del hogar";
            case "OFICINA" -> "Limpieza de oficina";
            case "EVENTO" -> "Limpieza de evento";
            default -> "Servicio de limpieza";
        };
    }

    private Cell celdaCabecera(String texto, TextAlignment alineacion) {
        return new Cell().add(new Paragraph(texto).setBold().setFontSize(8.5f))
                .setBackgroundColor(CREMA_OSCURO)
                .setFontColor(GRIS_TITULO)
                .setTextAlignment(alineacion)
                .setPadding(8)
                .setBorder(Border.NO_BORDER);
    }

    private Cell celdaFila(String texto, TextAlignment alineacion, boolean alterna) {
        return new Cell().add(new Paragraph(texto).setFontSize(9.5f))
                .setBackgroundColor(alterna ? CREMA_CLARO : CREMA_FONDO)
                .setTextAlignment(alineacion)
                .setPadding(8)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDE, 0.5f));
    }

    private Table construirFilaInferior(Factura factura) {
        Table contenedor = new Table(UnitValue.createPercentArray(new float[] { 48, 52 }))
                .useAllAvailableWidth().setMarginTop(20);

        Div pago = new Div();
        pago.add(new Paragraph("DETALLES DE PAGO").setBold().setFontSize(10).setFontColor(GRIS_TITULO)
                .setMarginBottom(5));
        if (!banco.isBlank()) {
            pago.add(new Paragraph("Banco: " + banco).setFontSize(9).setFontColor(GRIS));
        }
        if (!cuentaBancaria.isBlank()) {
            pago.add(new Paragraph("N.º de cuenta: " + cuentaBancaria).setFontSize(9).setFontColor(GRIS));
        }

        BigDecimal subtotal = factura.getSubtotal() != null ? factura.getSubtotal() : factura.getImporte();
        BigDecimal descuentoPct = factura.getDescuentoPorcentaje() != null ? factura.getDescuentoPorcentaje()
                : BigDecimal.ZERO;
        BigDecimal importeDescuento = subtotal.multiply(descuentoPct).divide(BigDecimal.valueOf(100));
        BigDecimal iva = factura.getIvaImporte() != null ? factura.getIvaImporte() : BigDecimal.ZERO;

        Table totales = new Table(UnitValue.createPercentArray(new float[] { 55, 45 })).useAllAvailableWidth();
        filaTotal(totales, "BASE IMPONIBLE", String.format("%.2f €", subtotal), false);
        if (descuentoPct.compareTo(BigDecimal.ZERO) > 0) {
            filaTotal(totales, "DESCUENTO (" + descuentoPct.stripTrailingZeros().toPlainString() + "%)",
                    "-" + String.format("%.2f €", importeDescuento), false);
        }
        filaTotal(totales, "IVA (21%)", String.format("%.2f €", iva), false);
        filaTotal(totales, "TOTAL", String.format("%.2f €", factura.getImporte()), true);

        contenedor.addCell(
                new Cell().add(pago).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.BOTTOM));
        contenedor.addCell(new Cell().add(totales).setBorder(Border.NO_BORDER).setPadding(0));

        return contenedor;
    }

    private void filaTotal(Table tabla, String etiqueta, String valor, boolean destacado) {
        Paragraph pEtiqueta = new Paragraph(etiqueta).setFontSize(destacado ? 12 : 9);
        Paragraph pValor = new Paragraph(valor).setFontSize(destacado ? 12 : 9).setTextAlignment(TextAlignment.RIGHT);
        if (destacado) {
            pEtiqueta.setBold();
            pValor.setBold();
        } else {
            pEtiqueta.setFontColor(GRIS);
        }

        Cell celdaEtiqueta = new Cell().add(pEtiqueta).setPadding(7).setBorder(Border.NO_BORDER);
        Cell celdaValor = new Cell().add(pValor).setPadding(7).setBorder(Border.NO_BORDER);

        if (destacado) {
            celdaEtiqueta.setBackgroundColor(CREMA_OSCURO);
            celdaValor.setBackgroundColor(CREMA_OSCURO);
        } else {
            celdaEtiqueta.setBackgroundColor(CREMA_CLARO);
            celdaValor.setBackgroundColor(CREMA_CLARO);
        }

        tabla.addCell(celdaEtiqueta);
        tabla.addCell(celdaValor);
    }

    private Paragraph construirPie() {
        return new Paragraph("¡GRACIAS POR SU CONFIANZA!")
                .setBold().setFontSize(13).setFontColor(GRIS_TITULO)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(40);
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