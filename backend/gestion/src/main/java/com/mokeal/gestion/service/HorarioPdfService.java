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
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.mokeal.gestion.model.Empleado;
import com.mokeal.gestion.model.EstadoServicio;
import com.mokeal.gestion.model.Servicio;
import com.mokeal.gestion.repository.ServicioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HorarioPdfService {

    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_CORTO = DateTimeFormatter.ofPattern("EEE d MMM", ES);
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("d MMM yyyy", ES);
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES);

    private static final DeviceRgb AZUL = new DeviceRgb(102, 153, 255);
    private static final DeviceRgb ZEBRA_AZUL = new DeviceRgb(243, 247, 255);
    private static final DeviceRgb ZEBRA_GRIS = new DeviceRgb(245, 245, 245);
    private static final DeviceRgb TEXTO = new DeviceRgb(51, 51, 51);
    private static final DeviceRgb GRIS = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb TITULO = new DeviceRgb(26, 26, 26);

    private final ServicioRepository servicioRepository;

    @Value("${mokeal.empresa.marca:Mokeal Limpieza}")
    private String marca;

    public HorarioPdfService(ServicioRepository servicioRepository) {
        this.servicioRepository = servicioRepository;
    }

    // ---------- Horario general (mes completo) ----------

    public byte[] generarGeneral(LocalDate desde, LocalDate hasta) {
        List<Servicio> servicios = servicioRepository.findByFechaBetween(desde, hasta).stream()
                .sorted(Comparator.comparing(Servicio::getFecha).thenComparing(Servicio::getHoraInicio))
                .toList();

        PdfFont normal = fuente(StandardFonts.HELVETICA);
        PdfFont negrita = fuente(StandardFonts.HELVETICA_BOLD);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(salida))) {
            pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE, barraSuperior());

            Document doc = new Document(pdfDoc, PageSize.A4.rotate());
            doc.setMargins(44, 40, 36, 40);
            doc.setFont(normal);
            doc.setFontSize(9);

            doc.add(cabecera(tituloPeriodo(desde, hasta), "Todos los trabajadores", negrita));

            if (servicios.isEmpty()) {
                doc.add(new Paragraph("No hay servicios en este periodo.")
                        .setFontColor(GRIS).setFontSize(10).setMarginTop(24));
            } else {
                Table tabla = new Table(UnitValue.createPercentArray(new float[]{9, 11, 8, 12, 17, 16, 17, 10}))
                        .useAllAvailableWidth().setMarginTop(16);

                for (String t : new String[]{"Fecha", "Horario", "Duración", "Cliente", "Dirección",
                        "Servicio", "Empleados", "Estado"}) {
                    tabla.addHeaderCell(celdaCabecera(t, negrita));
                }

                boolean zebra = false;
                for (Servicio s : servicios) {
                    String empleados = s.getEmpleados().isEmpty() ? "Sin asignar" : nombresEmpleados(s);
                    tabla.addCell(celda(diaCorto(s.getFecha()), zebra, ZEBRA_AZUL));
                    tabla.addCell(celda(horario(s), zebra, ZEBRA_AZUL));
                    tabla.addCell(celda(duracion(minutosDe(s)), zebra, ZEBRA_AZUL));
                    tabla.addCell(celda(s.getCliente().getNombre(), zebra, ZEBRA_AZUL));
                    tabla.addCell(celda(s.getDireccion(), zebra, ZEBRA_AZUL));
                    tabla.addCell(celda(nombreServicio(s), zebra, ZEBRA_AZUL));
                    tabla.addCell(celda(empleados, zebra, ZEBRA_AZUL));
                    tabla.addCell(celda(estado(s.getEstado()), zebra, ZEBRA_AZUL));
                    zebra = !zebra;
                }
                doc.add(tabla);
            }

            doc.close();
        }

        return salida.toByteArray();
    }

    // ---------- Un trabajador por página (día o semana) ----------

    public byte[] generarPorTrabajador(LocalDate desde, LocalDate hasta) {
        List<Servicio> servicios = servicioRepository.findByFechaBetween(desde, hasta).stream()
                .filter(s -> s.getEstado() != EstadoServicio.CANCELADO)
                .toList();

        Map<Long, Empleado> empleados = new HashMap<>();
        Map<Long, List<Servicio>> porEmpleado = new HashMap<>();
        for (Servicio s : servicios) {
            for (Empleado e : s.getEmpleados()) {
                empleados.putIfAbsent(e.getId(), e);
                porEmpleado.computeIfAbsent(e.getId(), k -> new ArrayList<>()).add(s);
            }
        }

        List<Long> ids = new ArrayList<>(porEmpleado.keySet());
        ids.sort(Comparator.comparing((Long id) -> empleados.get(id).getNombre()));

        PdfFont normal = fuente(StandardFonts.HELVETICA);
        PdfFont negrita = fuente(StandardFonts.HELVETICA_BOLD);

        String subtitulo = desde.equals(hasta)
                ? mayuscula(desde.getDayOfWeek().getDisplayName(TextStyle.FULL, ES)) + ", " + desde.format(FECHA_LARGA)
                : "Del " + fechaCorta(desde) + " al " + fechaCorta(hasta);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(salida))) {
            pdfDoc.addEventHandler(PdfDocumentEvent.START_PAGE, barraSuperior());

            Document doc = new Document(pdfDoc, PageSize.A4.rotate());
            doc.setMargins(44, 40, 36, 40);
            doc.setFont(normal);
            doc.setFontSize(9);

            if (ids.isEmpty()) {
                doc.add(cabecera("Planning", subtitulo, negrita));
                doc.add(new Paragraph("No hay servicios asignados a ningún trabajador en este periodo.")
                        .setFontColor(GRIS).setFontSize(10).setMarginTop(24));
            }

            boolean primero = true;
            for (Long id : ids) {
                if (!primero) doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                primero = false;

                List<Servicio> suyos = porEmpleado.get(id).stream()
                        .sorted(Comparator.comparing(Servicio::getFecha).thenComparing(Servicio::getHoraInicio))
                        .toList();
                long total = suyos.stream().mapToLong(this::minutosDe).sum();

                doc.add(cabecera(empleados.get(id).getNombre(), subtitulo, negrita));

                Table tabla = new Table(UnitValue.createPercentArray(new float[]{11, 12, 9, 17, 25, 17, 9}))
                        .useAllAvailableWidth().setMarginTop(16);

                for (String t : new String[]{"Fecha", "Horario", "Duración", "Cliente", "Dirección", "Servicio", "Estado"}) {
                    tabla.addHeaderCell(celdaCabecera(t, negrita));
                }

                boolean zebra = false;
                for (Servicio s : suyos) {
                    tabla.addCell(celda(diaCorto(s.getFecha()), zebra, ZEBRA_GRIS));
                    tabla.addCell(celda(horario(s), zebra, ZEBRA_GRIS));
                    tabla.addCell(celda(duracion(minutosDe(s)), zebra, ZEBRA_GRIS));
                    tabla.addCell(celda(s.getCliente().getNombre(), zebra, ZEBRA_GRIS));
                    tabla.addCell(celda(s.getDireccion(), zebra, ZEBRA_GRIS));
                    tabla.addCell(celda(nombreServicio(s), zebra, ZEBRA_GRIS));
                    tabla.addCell(celda(estado(s.getEstado()), zebra, ZEBRA_GRIS));
                    zebra = !zebra;
                }

                tabla.addCell(celdaPie("", negrita));
                tabla.addCell(celdaPie("Total horas", negrita));
                tabla.addCell(celdaPie(horasMinutos(total), negrita));
                for (int i = 0; i < 4; i++) tabla.addCell(celdaPie("", negrita));

                doc.add(tabla);
            }

            doc.close();
        }

        return salida.toByteArray();
    }

    // ---------- Piezas ----------

    private Table cabecera(String titulo, String subtitulo, PdfFont negrita) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();

        Cell izq = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
        izq.add(new Paragraph(titulo).setFont(negrita).setFontSize(20).setFontColor(TITULO).setMarginBottom(2));
        izq.add(new Paragraph(subtitulo).setFontSize(10).setFontColor(GRIS));

        Cell der = new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingTop(4)
                .add(new Paragraph(marca).setFontSize(10).setFontColor(GRIS).setTextAlignment(TextAlignment.RIGHT));

        t.addCell(izq);
        t.addCell(der);
        return t;
    }

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

    private Cell celdaCabecera(String texto, PdfFont negrita) {
        return new Cell()
                .add(new Paragraph(texto).setFont(negrita).setFontSize(9).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(AZUL).setBorder(Border.NO_BORDER).setPadding(7);
    }

    private Cell celda(String texto, boolean zebra, DeviceRgb colorZebra) {
        Cell c = new Cell().add(new Paragraph(texto).setFontSize(9).setFontColor(TEXTO))
                .setBorder(Border.NO_BORDER).setPadding(7);
        if (zebra) c.setBackgroundColor(colorZebra);
        return c;
    }

    private Cell celdaPie(String texto, PdfFont negrita) {
        return new Cell()
                .add(new Paragraph(texto).setFont(negrita).setFontSize(9).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(AZUL).setBorder(Border.NO_BORDER).setPadding(7);
    }

    private PdfFont fuente(String nombre) {
        try {
            return PdfFontFactory.createFont(nombre);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear la fuente del PDF", e);
        }
    }

    // ---------- Datos y formato ----------

    // Tiempo real en el servicio: si van varios empleados, trabajan a la vez
    private long minutosDe(Servicio s) {
        return (s.getHoraFin().toSecondOfDay() - s.getHoraInicio().toSecondOfDay()) / 60;
    }

    private String horario(Servicio s) {
        return s.getHoraInicio().format(HORA) + " - " + s.getHoraFin().format(HORA);
    }

    private String nombresEmpleados(Servicio s) {
        return s.getEmpleados().stream().map(Empleado::getNombre).sorted().collect(Collectors.joining(", "));
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

    private String tituloPeriodo(LocalDate desde, LocalDate hasta) {
        boolean mesCompleto = desde.getDayOfMonth() == 1 && hasta.equals(YearMonth.from(desde).atEndOfMonth());
        if (mesCompleto) {
            return "Horario " + desde.getMonth().getDisplayName(TextStyle.FULL, ES) + " " + desde.getYear();
        }
        return "Horario " + fechaCorta(desde) + " - " + fechaCorta(hasta);
    }

    private String diaCorto(LocalDate f) {
        return f.format(DIA_CORTO).replace(".", "");
    }

    private String fechaCorta(LocalDate f) {
        return f.format(FECHA_CORTA).replace(".", "");
    }

    private String mayuscula(String t) {
        return t.isEmpty() ? t : t.substring(0, 1).toUpperCase(ES) + t.substring(1);
    }

    private String horasMinutos(long minutos) {
        return (minutos / 60) + "h " + (minutos % 60) + "m";
    }

    private String duracion(long minutos) {
        return minutos % 60 == 0 ? (minutos / 60) + "h" : (minutos / 60) + "h " + (minutos % 60) + "m";
    }
}