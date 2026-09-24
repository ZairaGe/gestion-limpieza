package com.mokeal.gestion.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;

import org.springframework.core.io.ClassPathResource;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

@Service
public class FacturaPdfService {

        private final com.mokeal.gestion.repository.FacturaRepository facturaRepository;

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

        private static final DeviceRgb AZUL_PRIMARIO = new DeviceRgb(59, 111, 224);
        private static final DeviceRgb AZUL_SUAVE = new DeviceRgb(234, 240, 253);
        private static final DeviceRgb CREMA = new DeviceRgb(247, 243, 236);
        private static final DeviceRgb GRIS = new DeviceRgb(107, 107, 100);
        private static final DeviceRgb NEGRO = new DeviceRgb(26, 26, 26);
        private static final DeviceRgb BORDE = new DeviceRgb(214, 208, 196);

        public FacturaPdfService(com.mokeal.gestion.repository.FacturaRepository facturaRepository) {
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
                        documento.setMargins(40, 40, 40, 40);

                        documento.add(construirCabecera(factura));
                        documento.add(construirCajaFacturaFecha(factura));
                        documento.add(construirCajaCliente(factura));
                        documento.add(construirTablaConcepto(factura, servicios));
                        documento.add(construirTotales(factura));
                        documento.add(construirPie());

                        documento.close();
                }

                return salida.toByteArray();
        }

        private Image cargarLogo() {
                try (java.io.InputStream is = getClass().getResourceAsStream("/static/logo-mokeal.png")) {
                        if (is == null)
                                return null;
                        byte[] bytes = is.readAllBytes();
                        return new Image(ImageDataFactory.create(bytes)).setHeight(50);
                } catch (Exception e) {
                        System.err.println("No se pudo cargar el logo: " + e.getMessage());
                        return null;
                }
        }

        private Table construirCabecera(Factura factura) {
                Table fila = new Table(
                                UnitValue.createPercentArray(new float[] { 30, 70 })).useAllAvailableWidth();

                Table logoBox = new Table(1)
                                .setWidth(UnitValue.createPointValue(100));

                try {
                        ClassPathResource recurso = new ClassPathResource("logo.png");

                        ImageData imageData = ImageDataFactory.create(
                                        recurso.getInputStream().readAllBytes());

                        Image logo = new Image(imageData);
                        logo.setWidth(90);
                        logo.setAutoScaleHeight(true);

                        logoBox.addCell(
                                        new Cell()
                                                        .add(logo)
                                                        .setBorder(Border.NO_BORDER)
                                                        .setTextAlignment(TextAlignment.LEFT)
                                                        .setVerticalAlignment(VerticalAlignment.MIDDLE));

                } catch (Exception e) {
                        System.err.println("ERROR cargando logo: " + e.getMessage());
                        e.printStackTrace();
                        logoBox.addCell(
                                        new Cell()
                                                        .add(new Paragraph("Mokeal")
                                                                        .setBold()
                                                                        .setFontSize(14))
                                                        .setBorder(Border.NO_BORDER));
                }

                Cell celdaLogo = new Cell()
                                .setBorder(Border.NO_BORDER)
                                .add(logoBox);

                Cell celdaEmpresa = new Cell()
                                .setBorder(Border.NO_BORDER)
                                .setTextAlignment(TextAlignment.RIGHT);

                celdaEmpresa.add(
                                new Paragraph(empresaNombre)
                                                .setBold()
                                                .setFontSize(12)
                                                .setFontColor(NEGRO));

                if (!empresaNif.isBlank())
                        celdaEmpresa.add(new Paragraph(empresaNif)
                                        .setFontSize(9)
                                        .setFontColor(GRIS));

                if (!empresaDireccion.isBlank())
                        celdaEmpresa.add(new Paragraph(empresaDireccion)
                                        .setFontSize(9)
                                        .setFontColor(GRIS));

                if (!empresaTelefono.isBlank())
                        celdaEmpresa.add(new Paragraph(empresaTelefono)
                                        .setFontSize(9)
                                        .setFontColor(GRIS));

                if (!empresaEmail.isBlank())
                        celdaEmpresa.add(new Paragraph(empresaEmail)
                                        .setFontSize(9)
                                        .setFontColor(GRIS));

                fila.addCell(celdaLogo);
                fila.addCell(celdaEmpresa);

                return fila;
        }

        private Table construirCajaFacturaFecha(Factura factura) {
                Table contenedor = new Table(UnitValue.createPercentArray(new float[] { 60, 40 }))
                                .useAllAvailableWidth().setMarginTop(15);
                contenedor.addCell(new Cell().setBorder(Border.NO_BORDER));

                Table caja = new Table(UnitValue.createPercentArray(new float[] { 45, 55 }))
                                .useAllAvailableWidth()
                                .setBorder(new SolidBorder(BORDE, 0.7f));

                caja.addCell(celdaEtiquetaCaja("Factura"));
                caja.addCell(celdaValorCaja(factura.getNumero()));
                caja.addCell(celdaEtiquetaCaja("Fecha"));
                caja.addCell(celdaValorCaja(factura.getFechaEmision().toString()));

                contenedor.addCell(new Cell().add(caja).setBorder(Border.NO_BORDER).setPadding(0));
                return contenedor;
        }

        private Cell celdaEtiquetaCaja(String texto) {
                return new Cell().add(new Paragraph(texto).setBold().setFontSize(9))
                                .setBackgroundColor(AZUL_SUAVE)
                                .setPadding(5)
                                .setBorder(new SolidBorder(BORDE, 0.5f));
        }

        private Cell celdaValorCaja(String texto) {
                return new Cell().add(new Paragraph(texto).setFontSize(9))
                                .setPadding(5)
                                .setBorder(new SolidBorder(BORDE, 0.5f));
        }

        private Div construirCajaCliente(Factura factura) {
                Div caja = new Div()
                                .setBorder(new SolidBorder(BORDE, 0.7f))
                                .setPadding(12)
                                .setMarginTop(15);

                caja.add(new Paragraph("CLIENTE").setBold().setFontSize(9).setMarginBottom(4));

                Table datos = new Table(UnitValue.createPercentArray(new float[] { 20, 80 })).useAllAvailableWidth();
                datos.addCell(etiquetaSinBorde("Nombre:"));
                datos.addCell(valorSinBorde(factura.getCliente().getNombre(), true));

                if (factura.getCliente().getNif() != null && !factura.getCliente().getNif().isBlank()) {
                        datos.addCell(etiquetaSinBorde("CIF/NIF:"));
                        datos.addCell(valorSinBorde(factura.getCliente().getNif(), true));
                }

                if (factura.getCliente().getDireccion() != null && !factura.getCliente().getDireccion().isBlank()) {
                        String direccionCompleta = factura.getCliente().getDireccion();
                        if (factura.getCliente().getCodigoPostal() != null
                                        && !factura.getCliente().getCodigoPostal().isBlank()) {
                                direccionCompleta += ", " + factura.getCliente().getCodigoPostal();
                        }
                        datos.addCell(etiquetaSinBorde("Dirección:"));
                        datos.addCell(valorSinBorde(direccionCompleta, false));
                }

                caja.add(datos);
                return caja;
        }

        private Cell etiquetaSinBorde(String texto) {
                return new Cell().add(new Paragraph(texto).setFontSize(9).setFontColor(GRIS))
                                .setBorder(Border.NO_BORDER).setPaddingBottom(3);
        }

        private Cell valorSinBorde(String texto, boolean negrita) {
                Paragraph p = new Paragraph(texto).setFontSize(9);
                if (negrita)
                        p.setBold();
                return new Cell().add(p).setBorder(Border.NO_BORDER).setPaddingBottom(3);
        }

        private Table construirTablaConcepto(Factura factura, List<Servicio> servicios) {
                double horasTotales = servicios.stream()
                                .mapToDouble(this::obtenerDuracion)
                                .sum();

                BigDecimal subtotal = factura.getSubtotal() != null ? factura.getSubtotal() : factura.getImporte();
                String horasTexto = horasTotales > 0 ? String.format("%.2f", horasTotales) : "-";
                String precioHoraTexto = horasTotales > 0
                                ? String.format("%.2f €", subtotal.doubleValue() / horasTotales)
                                : "-";

                Table tabla = new Table(UnitValue.createPercentArray(new float[] { 45, 18, 18, 19 }))
                                .useAllAvailableWidth().setMarginTop(20);

                tabla.addHeaderCell(celdaCabecera("CONCEPTO", TextAlignment.LEFT));
                tabla.addHeaderCell(celdaCabecera("HORAS", TextAlignment.CENTER));
                tabla.addHeaderCell(celdaCabecera("€/HORA", TextAlignment.CENTER));
                tabla.addHeaderCell(celdaCabecera("SUBTOTAL", TextAlignment.RIGHT));

                String concepto = factura.getConcepto() != null && !factura.getConcepto().isBlank()
                                ? factura.getConcepto()
                                : "Servicios de limpieza";

                tabla.addCell(celdaFila(concepto, TextAlignment.LEFT));
                tabla.addCell(celdaFila(horasTexto, TextAlignment.CENTER));
                tabla.addCell(celdaFila(precioHoraTexto, TextAlignment.CENTER));
                tabla.addCell(celdaFila(String.format("%.2f €", subtotal), TextAlignment.RIGHT));

                return tabla;
        }

        private Cell celdaCabecera(String texto, TextAlignment alineacion) {
                return new Cell().add(new Paragraph(texto).setBold().setFontSize(9))
                                .setBackgroundColor(AZUL_SUAVE)
                                .setTextAlignment(alineacion)
                                .setPadding(7)
                                .setBorder(new SolidBorder(BORDE, 0.5f));
        }

        private Cell celdaFila(String texto, TextAlignment alineacion) {
                return new Cell().add(new Paragraph(texto).setFontSize(9.5f))
                                .setTextAlignment(alineacion)
                                .setPadding(7)
                                .setBorder(new SolidBorder(BORDE, 0.5f));
        }

        private Table construirTotales(Factura factura) {
                BigDecimal subtotal = factura.getSubtotal() != null ? factura.getSubtotal() : factura.getImporte();
                BigDecimal descuentoPct = factura.getDescuentoPorcentaje() != null ? factura.getDescuentoPorcentaje()
                                : BigDecimal.ZERO;
                BigDecimal importeDescuento = subtotal.multiply(descuentoPct).divide(BigDecimal.valueOf(100));
                BigDecimal baseFinal = subtotal.subtract(importeDescuento);
                BigDecimal iva = factura.getIvaImporte() != null ? factura.getIvaImporte() : BigDecimal.ZERO;

                Table contenedor = new Table(UnitValue.createPercentArray(new float[] { 55, 45 }))
                                .useAllAvailableWidth().setMarginTop(15);
                contenedor.addCell(new Cell().setBorder(Border.NO_BORDER));

                Table caja = new Table(UnitValue.createPercentArray(new float[] { 55, 45 }))
                                .useAllAvailableWidth()
                                .setBorder(new SolidBorder(BORDE, 0.7f));

                filaTotal(caja, "BASE IMPONIBLE", String.format("%.2f €", subtotal), false);
                filaTotal(caja, "DESCUENTO %", descuentoPct.stripTrailingZeros().toPlainString() + "%", false);
                filaTotal(caja, "DESCUENTO €", String.format("%.2f €", importeDescuento), false);
                filaTotal(caja, "BASE FINAL", String.format("%.2f €", baseFinal), false);
                filaTotal(caja, "IVA 21%", String.format("%.2f €", iva), false);
                filaTotal(caja, "TOTAL", String.format("%.2f €", factura.getImporte()), true);

                contenedor.addCell(new Cell().add(caja).setBorder(Border.NO_BORDER).setPadding(0));
                return contenedor;
        }

        private void filaTotal(Table tabla, String etiqueta, String valor, boolean destacado) {
                Paragraph pEtiqueta = new Paragraph(etiqueta).setFontSize(destacado ? 11 : 9);
                Paragraph pValor = new Paragraph(valor).setFontSize(destacado ? 11 : 9)
                                .setTextAlignment(TextAlignment.RIGHT);
                if (destacado) {
                        pEtiqueta.setBold();
                        pValor.setBold();
                }

                Cell celdaEtiqueta = new Cell().add(pEtiqueta).setPadding(6).setBorder(new SolidBorder(BORDE, 0.4f));
                Cell celdaValor = new Cell().add(pValor).setPadding(6).setBorder(new SolidBorder(BORDE, 0.4f));

                if (destacado) {
                        celdaEtiqueta.setBackgroundColor(AZUL_PRIMARIO);
                        celdaValor.setBackgroundColor(AZUL_PRIMARIO);
                        celdaEtiqueta.setFontColor(ColorConstants.WHITE);
                        celdaValor.setFontColor(ColorConstants.WHITE);
                }

                tabla.addCell(celdaEtiqueta);
                tabla.addCell(celdaValor);
        }

        private Div construirPie() {
                Div pie = new Div().setMarginTop(35);
                pie.add(new Paragraph("GRACIAS POR SU CONFIANZA").setBold().setFontSize(10)
                                .setFontColor(AZUL_PRIMARIO));

                if (!cuentaBancaria.isBlank()) {
                        pie.add(new Paragraph("Ingresar al número de cuenta:").setFontSize(9).setFontColor(GRIS)
                                        .setMarginTop(6));
                        pie.add(new Paragraph(cuentaBancaria).setFontSize(10).setBold());
                }

                return pie;
        }

        private double obtenerDuracion(Servicio servicio) {
                return servicio.getDuracionHoras() != null
                                ? servicio.getDuracionHoras()
                                : (servicio.getHoraFin().toSecondOfDay() - servicio.getHoraInicio().toSecondOfDay())
                                                / 3600.0;
        }
}