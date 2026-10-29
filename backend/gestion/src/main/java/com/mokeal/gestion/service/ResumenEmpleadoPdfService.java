package com.mokeal.gestion.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.mokeal.gestion.model.Empleado;
import com.mokeal.gestion.model.EstadoServicio;
import com.mokeal.gestion.model.Servicio;
import com.mokeal.gestion.repository.EmpleadoRepository;
import com.mokeal.gestion.repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ResumenEmpleadoPdfService {

    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DeviceRgb AZUL = new DeviceRgb(102, 153, 255);
    private static final DeviceRgb FILA_GRIS = new DeviceRgb(245, 245, 245);
    private static final DeviceRgb TEXTO = new DeviceRgb(51, 51, 51);
    private static final DeviceRgb GRIS = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb TITULO = new DeviceRgb(26, 26, 26);

    private final ServicioRepository servicioRepository;
    private final EmpleadoRepository empleadoRepository;

    @Value("${mokeal.empresa.nombre:Mokeal}")
    private String empresaNombre;

    public ResumenEmpleadoPdfService(ServicioRepository servicioRepository, EmpleadoRepository empleadoRepository) {
        this.servicioRepository = servicioRepository;
        this.empleadoRepository = empleadoRepository;
    }

    public byte[] generar(Long empleadoId, YearMonth mes) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id: " + empleadoId));

        List<Servicio> servicios = servicioRepository.findByFechaBetween(mes.atDay(1), mes.atEndOfMonth()).stream()
                .filter(s -> s.getEmpleados().stream().anyMatch(e -> e.getId().equals(empleadoId)))
                .sorted(Comparator.comparing(Servicio::getFecha).thenComparing(Servicio::getHoraInicio))
                .toList();

        long trabajados = servicios.stream()
                .filter(s -> s.getEstado() == EstadoServicio.COMPLETADO)
                .mapToLong(this::minutosDe).sum();
        long programados = servicios.stream()
                .filter(s -> s.getEstado() != EstadoServicio.CANCELADO)
                .mapToLong(this::minutosDe).sum();

        PdfFont normal;
        PdfFont negrita;
        try {
            normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            negrita = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        } catch (IOException e) {
            throw new RuntimeException("No se pudieron crear las fuentes del PDF", e);
        }

        String nombreMes = mes.getMonth().getDisplayName(TextStyle.FULL, ES) + " " + mes.getYear();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(salida))) {
            pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE, barraSuperior());

            Document doc = new Document(pdfDoc, PageSize.A4.rotate());
            doc.setMargins(44, 40, 36, 40);
            doc.setFont(normal);
            doc.setFontSize(9);

            // Cabecera
            Table cabecera = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();

            Cell izq = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
            izq.add(new Paragraph(empleado.getNombre()).setFont(negrita).setFontSize(20)
                    .setFontColor(TITULO).setMarginBottom(2));
            izq.add(new Paragraph("Resumen mensual · " + nombreMes).setFontSize(10)
                    .setFontColor(GRIS).setMarginBottom(8));
            izq.add(new Paragraph("Horas trabajadas (completados): " + hm(trabajados)
                    + "     Horas programadas (sin cancelados): " + hm(programados))
                    .setFontSize(10).setFontColor(GRIS));

            Cell der = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingTop(4)
                    .add(new Paragraph(empresaNombre).setFontSize(10).setFontColor(GRIS)
                            .setTextAlignment(TextAlignment.RIGHT));

            cabecera.addCell(izq);
            cabecera.addCell(der);
            doc.add(cabecera);

            if (servicios.isEmpty()) {
                doc.add(new Paragraph("Este trabajador no tiene servicios asignados en este mes.")
                        .setFontColor(GRIS).setFontSize(10).setMarginTop(24));
            } else {
                Table tabla = new Table(UnitValue.createPercentArray(new float[]{11, 12, 9, 15, 21, 21, 11}))
                        .useAllAvailableWidth().setMarginTop(16);

                for (String t : new String[]{"Fecha", "Horario", "Duración", "Cliente", "Dirección", "Servicio", "Estado"}) {
                    tabla.addHeaderCell(new Cell()
                            .add(new Paragraph(t).setFont(negrita).setFontSize(9).setFontColor(ColorConstants.WHITE))
                            .setBackgroundColor(AZUL).setBorder(Border.NO_BORDER).setPadding(7));
                }

                boolean zebra = false;
                for (Servicio s : servicios) {
                    tabla.addCell(celda(fecha(s.getFecha()), zebra));
                    tabla.addCell(celda(hora(s.getHoraInicio().toString()) + " - " + hora(s.getHoraFin().toString()), zebra));
                    tabla.addCell(celda(duracion(minutosDe(s)), zebra));
                    tabla.addCell(celda(s.getCliente().getNombre(), zebra));
                    tabla.addCell(celda(s.getDireccion(), zebra));
                    tabla.addCell(celda(nombreServicio(s), zebra));
                    tabla.addCell(celda(estado(s.getEstado()), zebra));
                    zebra = !zebra;
                }

                // Fila final azul con el total de horas trabajadas
                tabla.addCell(pie("", negrita));
                tabla.addCell(pie("Horas trabajadas", negrita));
                tabla.addCell(pie(hm(trabajados), negrita));
                for (int i = 0; i < 4; i++) tabla.addCell(pie("", negrita));

                doc.add(tabla);
            }

            doc.close();
        }

        return salida.toByteArray();
    }

    // ---------- Piezas ----------

    private IEventHandler barraSuperior() {
        return event -> {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfPage page = docEvent.getPage();
            Rectangle tam = page.getPageSize();
            PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), docEvent.getDocument());
            canvas.setFillColor(AZUL).rectangle(tam.getLeft(), tam.getTop() - 14, tam.getWidth(), 14).fill();
            canvas.release();
        };
    }

    private Cell celda(String texto, boolean zebra) {
        Cell c = new Cell().add(new Paragraph(texto).setFontSize(9).setFontColor(TEXTO))
                .setBorder(Border.NO_BORDER).setPadding(7);
        if (zebra) c.setBackgroundColor(FILA_GRIS);
        return c;
    }

    private Cell pie(String texto, PdfFont negrita) {
        return new Cell().add(new Paragraph(texto).setFont(negrita).setFontSize(9).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(AZUL).setBorder(Border.NO_BORDER).setPadding(7);
    }

    // ---------- Datos ----------

    // Tiempo real en el que el empleado está en el servicio (si van varios, trabajan a la vez)
    private long minutosDe(Servicio s) {
        return (s.getHoraFin().toSecondOfDay() - s.getHoraInicio().toSecondOfDay()) / 60;
    }

    private String nombreServicio(Servicio s) {
        if (s.getTarifa() == null || s.getTarifa().getNombre() == null || s.getTarifa().getNombre().isBlank()) {
            return "Servicio de limpieza";
        }
        return s.getTarifa().getNombre();
    }

    private String estado(EstadoServicio e) {
        return switch (e) {
            case PENDIENTE -> "Pendiente";
            case CONFIRMADO -> "Confirmado";
            case COMPLETADO -> "Completado";
            case CANCELADO -> "Cancelado";
        };
    }

    private String hm(long minutos) {
        return (minutos / 60) + "h " + (minutos % 60) + "m";
    }

    private String duracion(long minutos) {
        return minutos % 60 == 0 ? (minutos / 60) + "h" : (minutos / 60) + "h " + (minutos % 60) + "m";
    }

    private String hora(String hhmmss) {
        return hhmmss.length() >= 5 ? hhmmss.substring(0, 5) : hhmmss;
    }

    private String fecha(LocalDate f) {
        String mes = f.format(DateTimeFormatter.ofPattern("MMM", ES)).replace(".", "");
        return f.getDayOfMonth() + " " + mes + " " + f.getYear();
    }
}