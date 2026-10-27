package com.mokeal.gestion.service;

import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.mokeal.gestion.model.LineaPresupuesto;
import com.mokeal.gestion.model.Presupuesto;
import com.mokeal.gestion.repository.PresupuestoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    @Value("${mokeal.empresa.cuentaBancaria:}")
    private String cuentaBancaria;

    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DeviceRgb FONDO = new DeviceRgb(240, 239, 235);
    private static final DeviceRgb NEGRO = new DeviceRgb(20, 20, 20);
    private static final DeviceRgb GRIS_TOTAL = new DeviceRgb(201, 201, 201);
    private static final DeviceRgb AZUL_LOGO = new DeviceRgb(102, 153, 255);

    // Mismas posiciones que FacturaPdfService (A4 = 595 x 842 puntos)
    private static final float IZQ = 53.5f;
    private static final float DER = 541.7f;
    private static final float COL_1 = 286.0f;
    private static final float COL_2 = 356.9f;
    private static final float COL_3 = 431.5f;
    private static final float CENTRO_HORAS = 321.4f;
    private static final float CENTRO_PRECIO = 395.0f;
    private static final float CENTRO_SUBTOTAL = 470.2f;
    private static final float X_CONCEPTO = 110.1f;
    private static final float ANCHO_CONCEPTO = 167.9f;

    public PresupuestoPdfService(PresupuestoRepository presupuestoRepository) {
        this.presupuestoRepository = presupuestoRepository;
    }

    public byte[] generarPdf(Long id) {
        Presupuesto p = presupuestoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Presupuesto no encontrado con id: " + id));

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(salida))) {
            PdfCanvas c = new PdfCanvas(pdfDoc.addNewPage(PageSize.A4));

            PdfFont negrita = cargarFuente("fonts/Montserrat-Bold.ttf", StandardFonts.HELVETICA_BOLD);
            PdfFont media = cargarFuente("fonts/Montserrat-Medium.ttf", StandardFonts.HELVETICA);
            PdfFont normal = cargarFuente("fonts/Questrial-Regular.ttf", StandardFonts.HELVETICA);

            c.setFillColor(FONDO)
                    .rectangle(0, 0, PageSize.A4.getWidth(), PageSize.A4.getHeight())
                    .fill();

            dibujarCabecera(c, normal);
            dibujarClienteYPresupuesto(c, p, negrita, normal);
            dibujarTablaConceptos(c, p.getLineas(), negrita, normal);
            dibujarNotas(c, p, negrita, normal);
            dibujarTotales(c, p, negrita, media);
            dibujarPie(c, negrita, normal);
        }

        return salida.toByteArray();
    }

    // ---------- Bloques ----------

    private void dibujarCabecera(PdfCanvas c, PdfFont normal) {
        float x = IZQ, y = 735.0f, lado = 78.6f;
        ImageData logo = null;

        try (InputStream inputStream = getClass().getResourceAsStream("/static/logofactura.png")) {

            if (inputStream != null) {
                logo = ImageDataFactory.create(inputStream.readAllBytes());
            } else {
                System.err.println("No se encuentra logofactura.png");
            }

        } catch (IOException e) {
            throw new RuntimeException("Error al cargar el logo de la factura", e);
        }

        c.saveState();
        c.roundRectangle(x, y, lado, lado, 13);
        c.clip();
        c.endPath();
        if (logo != null) {
            c.addImageFittedIntoRectangle(logo, new Rectangle(x, y, lado, lado), false);
        } else {
            c.setFillColor(AZUL_LOGO).rectangle(x, y, lado, lado).fill();
        }
        c.restoreState();

        List<String> datos = new ArrayList<>();
        datos.add(empresaNombre);
        if (!empresaNif.isBlank())
            datos.add(empresaNif);
        if (!empresaDireccion.isBlank())
            datos.add(empresaDireccion);
        if (!empresaTelefono.isBlank())
            datos.add(empresaTelefono);
        if (!empresaEmail.isBlank())
            datos.add(empresaEmail);

        float yy = 803.3f;
        for (String linea : datos) {
            texto(c, normal, 11, NEGRO, 151.4f, yy, linea);
            yy -= 16.4f;
        }
    }

    private void dibujarClienteYPresupuesto(PdfCanvas c, Presupuesto p, PdfFont negrita, PdfFont normal) {
        linea(c, IZQ, DER, 711.8f, 0.8f);

        texto(c, negrita, 10.5f, NEGRO, 58.6f, 693.3f, "CLIENTE");

        List<String> datos = new ArrayList<>();
        if (hay(p.getNombre()))
            datos.addAll(partir("Nombre: " + p.getNombre(), normal, 10.5f, 280f));
        if (hay(p.getTelefono()))
            datos.add("Teléfono: " + p.getTelefono());
        if (hay(p.getCorreo()))
            datos.addAll(partir("Correo: " + p.getCorreo(), normal, 10.5f, 280f));
        if (hay(p.getDireccion()))
            datos.addAll(partir("Dirección: " + p.getDireccion(), normal, 10.5f, 280f));
        if (datos.size() > 4)
            datos = new ArrayList<>(datos.subList(0, 4));

        // Con cuatro líneas se aprieta un poco el interlineado para no tocar la raya de
        // abajo
        boolean cuatro = datos.size() > 3;
        float paso = cuatro ? 13.2f : 16.4f;
        float yy = cuatro ? 676.0f : 674.4f;
        for (String l : datos) {
            texto(c, normal, 10.5f, NEGRO, 58.6f, yy, l);
            yy -= paso;
        }

        String etiqueta = "PRESUPUESTO Nº:";
        float w1 = negrita.getWidth(etiqueta, 11);
        float w2 = normal.getWidth(p.getNumero(), 11);
        float xr = Math.min(364.5f, DER - (w1 + 4 + w2));

        texto(c, negrita, 11, NEGRO, xr, 691.8f, etiqueta);
        texto(c, normal, 11, NEGRO, xr + w1 + 4, 691.8f, p.getNumero());
        texto(c, negrita, 11, NEGRO, xr, 670.6f, "Fecha:");
        texto(c, normal, 11, NEGRO, xr + negrita.getWidth("Fecha:", 11) + 4, 670.6f,
                p.getFechaEmision().format(FECHA));

        linea(c, IZQ, DER, 624.9f, 0.8f);
    }

    private void dibujarTablaConceptos(PdfCanvas c, List<LineaPresupuesto> lineas, PdfFont negrita, PdfFont normal) {
        float yCab = 586.9f;
        texto(c, negrita, 10.5f, NEGRO, X_CONCEPTO, yCab, "Concepto");
        centrado(c, negrita, 10.5f, NEGRO, CENTRO_HORAS, yCab, "Horas");
        centrado(c, negrita, 10.5f, NEGRO, CENTRO_PRECIO, yCab, "€/Hora");
        centrado(c, negrita, 10.5f, NEGRO, CENTRO_SUBTOTAL, yCab, "Subtotal");

        for (float x : new float[] { COL_1, COL_2, COL_3 }) {
            c.setStrokeColor(NEGRO).setLineWidth(0.8f).moveTo(x, 563.0f).lineTo(x, 380.1f).stroke();
        }

        c.setFillColor(NEGRO).rectangle(IZQ, 368.6f, DER - IZQ, 11.5f).fill();

        int n = lineas.size();
        float paso = n <= 5 ? 29.6f : Math.max(14f, (531.4f - 392f) / (n - 1));
        float tam = paso >= 20 ? 10f : 8.5f;

        for (int i = 0; i < n; i++) {
            LineaPresupuesto l = lineas.get(i);
            float y = 531.4f - i * paso;

            double horas = l.getHoras() != null ? l.getHoras() : 0;
            BigDecimal precio = l.getPrecioHora() != null ? l.getPrecioHora() : BigDecimal.ZERO;
            BigDecimal importe = l.getSubtotal() != null ? l.getSubtotal() : BigDecimal.ZERO;
            String etiqueta = (l.getConcepto() != null ? l.getConcepto() : "").toUpperCase(ES);

            if (n <= 5) {
                List<String> partes = partir(etiqueta, normal, tam, ANCHO_CONCEPTO);
                texto(c, normal, tam, NEGRO, X_CONCEPTO, y, partes.get(0));
                if (partes.size() > 1) {
                    String resto = String.join(" ", partes.subList(1, partes.size()));
                    texto(c, normal, tam, NEGRO, X_CONCEPTO, y - 12, recortar(resto, normal, tam, ANCHO_CONCEPTO));
                }
            } else {
                texto(c, normal, tam, NEGRO, X_CONCEPTO, y, recortar(etiqueta, normal, tam, ANCHO_CONCEPTO));
            }

            centrado(c, normal, tam, NEGRO, CENTRO_HORAS, y, formato(horas, 0, 2));
            centrado(c, normal, tam, NEGRO, CENTRO_PRECIO, y, libre(precio.doubleValue()) + "€");
            centrado(c, normal, tam, NEGRO, CENTRO_SUBTOTAL, y, libre(importe.doubleValue()) + "€");
        }
    }

    // Las notas van a la izquierda, bajo la barra negra, al lado de la caja de
    // totales
    private void dibujarNotas(PdfCanvas c, Presupuesto p, PdfFont negrita, PdfFont normal) {
        if (!hay(p.getNotas()))
            return;

        texto(c, negrita, 10.5f, NEGRO, IZQ, 345.0f, "Notas");
        List<String> partes = partir(p.getNotas().trim(), normal, 10f, 262f);
        float y = 329.0f;
        for (int i = 0; i < Math.min(partes.size(), 8); i++) {
            texto(c, normal, 10f, NEGRO, IZQ, y, partes.get(i));
            y -= 14f;
        }
    }

    private void dibujarTotales(PdfCanvas c, Presupuesto p, PdfFont negrita, PdfFont media) {
        BigDecimal subtotal = p.getSubtotal();
        BigDecimal pct = p.getDescuentoPorcentaje() != null ? p.getDescuentoPorcentaje() : BigDecimal.ZERO;
        BigDecimal descuento = subtotal.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal baseFinal = subtotal.subtract(descuento);
        BigDecimal ivaPct = p.getIvaPorcentaje() != null ? p.getIvaPorcentaje() : BigDecimal.ZERO;
        BigDecimal iva = p.getIvaImporte() != null ? p.getIvaImporte() : BigDecimal.ZERO;

        String[][] filas = {
                { "Base Imponible", dos(subtotal) + " €" },
                { "Descuento %", dos(pct) + " %" },
                { "Descuento €", dos(descuento) + " €" },
                { "Base final", dos(baseFinal) + " €" },
                { "IVA " + formato(ivaPct.doubleValue(), 0, 2) + " %", dos(iva) + " €" },
                { "TOTAL", dos(p.getTotal()) + " €" }
        };

        float izq = 339.4f, der = 519.7f, divisor = 433.4f, arriba = 368.6f, alto = 22.98f;

        c.setFillColor(GRIS_TOTAL).rectangle(izq, arriba - 6 * alto, der - izq, alto).fill();

        for (int i = 1; i <= 6; i++) {
            linea(c, izq, der, arriba - i * alto, i == 5 ? 2.2f : 0.8f);
        }
        c.setStrokeColor(NEGRO).setLineWidth(0.8f)
                .moveTo(divisor, arriba).lineTo(divisor, arriba - 6 * alto).stroke();

        for (int i = 0; i < filas.length; i++) {
            float base = arriba - i * alto - 15.2f;
            texto(c, i == 5 ? negrita : media, 10.5f, NEGRO, izq, base, filas[i][0]);
            derecha(c, negrita, 10.5f, NEGRO, der - 2f, base, filas[i][1]);
        }
    }

    private void dibujarPie(PdfCanvas c, PdfFont negrita, PdfFont normal) {
        texto(c, negrita, 11, NEGRO, IZQ, 108.4f, "INGRESAR AL NÚMERO DE CUENTA");
        if (!cuentaBancaria.isBlank()) {
            texto(c, normal, 11, NEGRO, IZQ, 82.7f, cuentaBancaria);
        }
        derecha(c, negrita, 10.5f, NEGRO, 539f, 82.7f, "¡GRACIAS POR SU CONFIANZA!");
    }

    // ---------- Dibujo ----------

    private void texto(PdfCanvas c, PdfFont fuente, float tam, Color color, float x, float y, String t) {
        c.setFillColor(color);
        c.beginText().setFontAndSize(fuente, tam).moveText(x, y).showText(t).endText();
    }

    private void centrado(PdfCanvas c, PdfFont fuente, float tam, Color color, float cx, float y, String t) {
        texto(c, fuente, tam, color, cx - fuente.getWidth(t, tam) / 2, y, t);
    }

    private void derecha(PdfCanvas c, PdfFont fuente, float tam, Color color, float xDer, float y, String t) {
        texto(c, fuente, tam, color, xDer - fuente.getWidth(t, tam), y, t);
    }

    private void linea(PdfCanvas c, float x1, float x2, float y, float grosor) {
        c.setStrokeColor(NEGRO).setLineWidth(grosor).moveTo(x1, y).lineTo(x2, y).stroke();
    }

    private List<String> partir(String texto, PdfFont fuente, float tam, float ancho) {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.trim().split("\\s+")) {
            String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (actual.length() == 0 || fuente.getWidth(prueba, tam) <= ancho) {
                actual = new StringBuilder(prueba);
            } else {
                lineas.add(actual.toString());
                actual = new StringBuilder(palabra);
            }
        }
        if (actual.length() > 0)
            lineas.add(actual.toString());
        if (lineas.isEmpty())
            lineas.add("");
        return lineas;
    }

    private String recortar(String t, PdfFont fuente, float tam, float ancho) {
        if (fuente.getWidth(t, tam) <= ancho)
            return t;
        String base = t;
        while (base.length() > 1 && fuente.getWidth(base + "...", tam) > ancho) {
            base = base.substring(0, base.length() - 1);
        }
        return base.trim() + "...";
    }

    private boolean hay(String s) {
        return s != null && !s.isBlank();
    }

    // ---------- Formato ----------

    private String formato(double valor, int min, int max) {
        NumberFormat nf = NumberFormat.getNumberInstance(ES);
        nf.setMinimumFractionDigits(min);
        nf.setMaximumFractionDigits(max);
        nf.setRoundingMode(RoundingMode.HALF_UP);
        return nf.format(valor);
    }

    private String dos(BigDecimal v) {
        return formato(v.doubleValue(), 2, 2);
    }

    private String libre(double v) {
        boolean entero = Math.abs(v - Math.rint(v)) < 0.005;
        return entero ? formato(v, 0, 0) : formato(v, 2, 2);
    }

    // ---------- Recursos ----------

    private ImageData cargarLogo() {
        try (InputStream is = new ClassPathResource("static/logo.png").getInputStream()) {
            return ImageDataFactory.create(is.readAllBytes());
        } catch (Exception e) {
            System.err.println("No se pudo cargar el logo: " + e.getMessage());
            return null;
        }
    }

    private PdfFont cargarFuente(String ruta, String respaldo) {
        try {
            ClassPathResource recurso = new ClassPathResource(ruta);
            if (recurso.exists()) {
                byte[] bytes;
                try (InputStream is = recurso.getInputStream()) {
                    bytes = is.readAllBytes();
                }
                return PdfFontFactory.createFont(FontProgramFactory.createFont(bytes),
                        PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
            }
        } catch (Exception e) {
            System.err.println("No se pudo cargar la fuente " + ruta + ": " + e.getMessage());
        }
        try {
            return PdfFontFactory.createFont(respaldo);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear la fuente de respaldo", e);
        }
    }
}