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
import com.mokeal.gestion.model.Presupuesto;
import com.mokeal.gestion.repository.PresupuestoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@Service
public class PresupuestoPdfService {

    private final PresupuestoRepository presupuestoRepository;

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

    private static final DeviceRgb AZUL_PRIMARIO = new DeviceRgb(59, 111, 224);
    private static final DeviceRgb AZUL_SUAVE = new DeviceRgb(234, 240, 253);
    private static final DeviceRgb CREMA = new DeviceRgb(247, 243, 236);
    private static final DeviceRgb GRIS = new DeviceRgb(107, 107, 100);
    private static final DeviceRgb NEGRO = new DeviceRgb(26, 26, 26);
    private static final DeviceRgb BORDE = new DeviceRgb(214, 208, 196);

    public PresupuestoPdfService(PresupuestoRepository presupuestoRepository) {
        this.presupuestoRepository = presupuestoRepository;
    }

    public byte[] generarPdf(Long id) {
        Presupuesto p = presupuestoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Presupuesto no encontrado con id: " + id));

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(salida))) {
            Document documento = new Document(pdfDoc, PageSize.A4);
            documento.setMargins(40, 40, 40, 40);
            documento.setBackgroundColor(CREMA);

            documento.add(construirCabecera(p));
            documento.add(construirCajaNumeroFecha(p));
            documento.add(construirCajaCliente(p));
            documento.add(construirTablaConcepto(p));
            documento.add(construirTotales(p));
            documento.add(construirPie());

            documento.close();
        }

        return salida.toByteArray();
    }

    private Table construirCabecera(Presupuesto p) {
        Table fila = new Table(UnitValue.createPercentArray(new float[] { 30, 70 })).useAllAvailableWidth();

        Table logoBox = new Table(1).setWidth(UnitValue.createPointValue(70));
        logoBox.addCell(new Cell()
                .add(new Paragraph("Mokeal").setFontColor(ColorConstants.WHITE).setBold().setFontSize(14)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(AZUL_PRIMARIO)
                .setBorder(Border.NO_BORDER)
                .setHeight(50)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        Cell celdaLogo = new Cell().setBorder(Border.NO_BORDER).add(logoBox);

        Cell celdaEmpresa = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        celdaEmpresa.add(new Paragraph(empresaNombre).setBold().setFontSize(12).setFontColor(NEGRO));
        if (!empresaNif.isBlank())
            celdaEmpresa.add(new Paragraph(empresaNif).setFontSize(9).setFontColor(GRIS));
        if (!empresaDireccion.isBlank())
            celdaEmpresa.add(new Paragraph(empresaDireccion).setFontSize(9).setFontColor(GRIS));
        if (!empresaTelefono.isBlank())
            celdaEmpresa.add(new Paragraph(empresaTelefono).setFontSize(9).setFontColor(GRIS));
        if (!empresaEmail.isBlank())
            celdaEmpresa.add(new Paragraph(empresaEmail).setFontSize(9).setFontColor(GRIS));

        fila.addCell(celdaLogo);
        fila.addCell(celdaEmpresa);
        return fila;
    }

    private Table construirCajaNumeroFecha(Presupuesto p) {
        Table contenedor = new Table(UnitValue.createPercentArray(new float[] { 60, 40 }))
                .useAllAvailableWidth().setMarginTop(15);
        contenedor.addCell(new Cell().setBorder(Border.NO_BORDER));

        Table caja = new Table(UnitValue.createPercentArray(new float[] { 45, 55 }))
                .useAllAvailableWidth().setBorder(new SolidBorder(BORDE, 0.7f));

        caja.addCell(celdaEtiquetaCaja("Presupuesto"));
        caja.addCell(celdaValorCaja(p.getNumero()));
        caja.addCell(celdaEtiquetaCaja("Fecha"));
        caja.addCell(celdaValorCaja(p.getFechaEmision().toString()));

        contenedor.addCell(new Cell().add(caja).setBorder(Border.NO_BORDER).setPadding(0));
        return contenedor;
    }

    private Cell celdaEtiquetaCaja(String texto) {
        return new Cell().add(new Paragraph(texto).setBold().setFontSize(9))
                .setBackgroundColor(AZUL_SUAVE).setPadding(5).setBorder(new SolidBorder(BORDE, 0.5f));
    }

    private Cell celdaValorCaja(String texto) {
        return new Cell().add(new Paragraph(texto).setFontSize(9))
                .setPadding(5).setBorder(new SolidBorder(BORDE, 0.5f));
    }

    private Div construirCajaCliente(Presupuesto p) {
        Div caja = new Div().setBorder(new SolidBorder(BORDE, 0.7f)).setPadding(12).setMarginTop(15);
        caja.add(new Paragraph("CLIENTE").setBold().setFontSize(9).setMarginBottom(4));

        Table datos = new Table(UnitValue.createPercentArray(new float[] { 20, 80 })).useAllAvailableWidth();
        datos.addCell(etiquetaSinBorde("Nombre:"));
        datos.addCell(valorSinBorde(p.getCliente().getNombre(), true));

        if (p.getCliente().getNif() != null && !p.getCliente().getNif().isBlank()) {
            datos.addCell(etiquetaSinBorde("CIF/NIF:"));
            datos.addCell(valorSinBorde(p.getCliente().getNif(), true));
        }

        if (p.getCliente().getDireccion() != null && !p.getCliente().getDireccion().isBlank()) {
            String direccionCompleta = p.getCliente().getDireccion();
            if (p.getCliente().getCodigoPostal() != null && !p.getCliente().getCodigoPostal().isBlank()) {
                direccionCompleta += ", " + p.getCliente().getCodigoPostal();
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
        Paragraph par = new Paragraph(texto).setFontSize(9);
        if (negrita)
            par.setBold();
        return new Cell().add(par).setBorder(Border.NO_BORDER).setPaddingBottom(3);
    }

    private Table construirTablaConcepto(Presupuesto p) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[] { 45, 18, 18, 19 }))
                .useAllAvailableWidth().setMarginTop(20);

        tabla.addHeaderCell(celdaCabecera("CONCEPTO", TextAlignment.LEFT));
        tabla.addHeaderCell(celdaCabecera("HORAS", TextAlignment.CENTER));
        tabla.addHeaderCell(celdaCabecera("€/HORA", TextAlignment.CENTER));
        tabla.addHeaderCell(celdaCabecera("SUBTOTAL", TextAlignment.RIGHT));

        for (com.mokeal.gestion.model.LineaPresupuesto linea : p.getLineas()) {
            tabla.addCell(celdaFila(linea.getConcepto(), TextAlignment.LEFT));
            tabla.addCell(celdaFila(String.format("%.2f", linea.getHoras()), TextAlignment.CENTER));
            tabla.addCell(celdaFila(String.format("%.2f €", linea.getPrecioHora()), TextAlignment.CENTER));
            tabla.addCell(celdaFila(String.format("%.2f €", linea.getSubtotal()), TextAlignment.RIGHT));
        }

        return tabla;
    }

    private Cell celdaCabecera(String texto, TextAlignment alineacion) {
        return new Cell().add(new Paragraph(texto).setBold().setFontSize(9))
                .setBackgroundColor(AZUL_SUAVE).setTextAlignment(alineacion).setPadding(7)
                .setBorder(new SolidBorder(BORDE, 0.5f));
    }

    private Cell celdaFila(String texto, TextAlignment alineacion) {
        return new Cell().add(new Paragraph(texto).setFontSize(9.5f))
                .setTextAlignment(alineacion).setPadding(7).setBorder(new SolidBorder(BORDE, 0.5f));
    }

    private Table construirTotales(Presupuesto p) {
        BigDecimal descuentoPct = p.getDescuentoPorcentaje() != null ? p.getDescuentoPorcentaje() : BigDecimal.ZERO;
        BigDecimal importeDescuento = p.getSubtotal().multiply(descuentoPct).divide(BigDecimal.valueOf(100));

        Table contenedor = new Table(UnitValue.createPercentArray(new float[] { 55, 45 }))
                .useAllAvailableWidth().setMarginTop(15);
        contenedor.addCell(new Cell().setBorder(Border.NO_BORDER));

        Table caja = new Table(UnitValue.createPercentArray(new float[] { 55, 45 }))
                .useAllAvailableWidth().setBorder(new SolidBorder(BORDE, 0.7f));

        filaTotal(caja, "SUBTOTAL", String.format("%.2f €", p.getSubtotal()), false);
        filaTotal(caja, "DESCUENTO %", descuentoPct.stripTrailingZeros().toPlainString() + "%", false);
        filaTotal(caja, "DESCUENTO €", String.format("%.2f €", importeDescuento), false);
        filaTotal(caja, "TOTAL ESTIMADO", String.format("%.2f €", p.getTotal()), true);

        contenedor.addCell(new Cell().add(caja).setBorder(Border.NO_BORDER).setPadding(0));
        return contenedor;
    }

    private void filaTotal(Table tabla, String etiqueta, String valor, boolean destacado) {
        Paragraph pEtiqueta = new Paragraph(etiqueta).setFontSize(destacado ? 11 : 9);
        Paragraph pValor = new Paragraph(valor).setFontSize(destacado ? 11 : 9).setTextAlignment(TextAlignment.RIGHT);
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
        pie.add(new Paragraph("Presupuesto sin IVA incluido. Válido salvo confirmación de disponibilidad.")
                .setFontSize(9).setFontColor(GRIS));
        pie.add(new Paragraph("GRACIAS POR CONFIAR EN NOSOTROS").setBold().setFontSize(10)
                .setFontColor(AZUL_PRIMARIO).setMarginTop(10));
        return pie;
    }
}