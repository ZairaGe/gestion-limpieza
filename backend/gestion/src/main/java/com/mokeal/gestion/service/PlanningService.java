package com.mokeal.gestion.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.mokeal.gestion.model.Empleado;
import com.mokeal.gestion.model.Servicio;
import com.mokeal.gestion.repository.EmpleadoRepository;
import com.mokeal.gestion.repository.ServicioRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlanningService {

    private final ServicioRepository servicioRepository;
    private final EmpleadoRepository empleadoRepository;

    public PlanningService(ServicioRepository servicioRepository, EmpleadoRepository empleadoRepository) {
        this.servicioRepository = servicioRepository;
        this.empleadoRepository = empleadoRepository;
    }

    public byte[] generarPlanningDiario(Long empleadoId, LocalDate fecha) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado con id: " + empleadoId));

        List<Servicio> servicios = servicioRepository.findByFecha(fecha).stream()
                .filter(s -> s.getEmpleados().stream().anyMatch(e -> e.getId().equals(empleadoId)))
                .sorted((a, b) -> a.getHoraInicio().compareTo(b.getHoraInicio()))
                .collect(Collectors.toList());

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(salida))) {
            Document documento = new Document(pdfDoc);

            String fechaFormateada = fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "ES"))
                    + ", " + fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "ES")));

            documento.add(new Paragraph("Planning de " + empleado.getNombre())
                    .setFontSize(18).setBold());
            documento.add(new Paragraph(fechaFormateada)
                    .setFontSize(11).setFontColor(ColorConstants.GRAY).setMarginBottom(15));

            if (servicios.isEmpty()) {
                documento.add(new Paragraph("No hay servicios asignados para este día."));
            } else {
                double totalHorasDia = 0;

                Table tabla = new Table(UnitValue.createPercentArray(new float[] { 18, 25, 42, 15 }))
                        .useAllAvailableWidth();

                tabla.addHeaderCell(celdaCabecera("Hora"));
                tabla.addHeaderCell(celdaCabecera("Cliente"));
                tabla.addHeaderCell(celdaCabecera("Dirección"));
                tabla.addHeaderCell(celdaCabecera("Horas"));

                for (Servicio servicio : servicios) {
                    double horasServicio = calcularHorasPorEmpleado(servicio);
                    totalHorasDia += horasServicio;

                    tabla.addCell(new Cell().add(new Paragraph(
                            servicio.getHoraInicio() + " - " + servicio.getHoraFin())));
                    tabla.addCell(new Cell().add(new Paragraph(servicio.getCliente().getNombre())));
                    tabla.addCell(new Cell().add(new Paragraph(servicio.getDireccion())));
                    tabla.addCell(new Cell().add(new Paragraph(String.format("%.2fh", horasServicio))));
                }

                documento.add(tabla);
                documento.add(new Paragraph("Total horas del día: " + String.format("%.2fh", totalHorasDia))
                        .setBold().setMarginTop(10));
            }

            documento.close();
        }

        return salida.toByteArray();
    }

    public byte[] generarPlanningGeneral(LocalDate desde, LocalDate hasta) {
        List<Servicio> servicios = servicioRepository.findByFechaBetween(desde, hasta).stream()
                .sorted((a, b) -> {
                    int cmp = a.getFecha().compareTo(b.getFecha());
                    return cmp != 0 ? cmp : a.getHoraInicio().compareTo(b.getHoraInicio());
                })
                .collect(Collectors.toList());

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(salida))) {
            Document documento = new Document(pdfDoc);

            documento.add(new Paragraph("Planning general").setFontSize(18).setBold());
            documento.add(new Paragraph(desde + "  →  " + hasta)
                    .setFontSize(11).setFontColor(ColorConstants.GRAY).setMarginBottom(15));

            Map<LocalDate, List<Servicio>> porDia = servicios.stream()
                    .collect(Collectors.groupingBy(Servicio::getFecha, LinkedHashMap::new, Collectors.toList()));

            if (porDia.isEmpty()) {
                documento.add(new Paragraph("No hay servicios en este rango de fechas."));
            }

            for (Map.Entry<LocalDate, List<Servicio>> entrada : porDia.entrySet()) {
                LocalDate fecha = entrada.getKey();
                String etiquetaDia = fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "ES"))
                        + ", " + fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM", new Locale("es", "ES")));

                documento.add(new Paragraph(etiquetaDia).setFontSize(13).setBold().setMarginTop(12));

                Table tabla = new Table(UnitValue.createPercentArray(new float[] { 13, 20, 27, 25, 15 }))
                        .useAllAvailableWidth();
                tabla.addHeaderCell(celdaCabecera("Hora"));
                tabla.addHeaderCell(celdaCabecera("Cliente"));
                tabla.addHeaderCell(celdaCabecera("Dirección"));
                tabla.addHeaderCell(celdaCabecera("Empleados"));
                tabla.addHeaderCell(celdaCabecera("Horas/empleado"));

                for (Servicio s : entrada.getValue()) {
                    double horasCadaUno = calcularHorasPorEmpleado(s);

                    tabla.addCell(new Cell().add(new Paragraph(s.getHoraInicio() + " - " + s.getHoraFin())));
                    tabla.addCell(new Cell().add(new Paragraph(s.getCliente().getNombre())));
                    tabla.addCell(new Cell().add(new Paragraph(s.getDireccion())));

                    String nombresEmpleados = s.getEmpleados().isEmpty() ? "Sin asignar"
                            : s.getEmpleados().stream().map(Empleado::getNombre).collect(Collectors.joining(", "));
                    tabla.addCell(new Cell().add(new Paragraph(nombresEmpleados)));
                    tabla.addCell(new Cell().add(new Paragraph(
                            s.getEmpleados().isEmpty() ? "—" : String.format("%.2fh", horasCadaUno))));
                }

                documento.add(new Paragraph(
                        "Nota: cuando un servicio tiene varios empleados asignados, todos trabajan a la vez durante el horario indicado. "
                                + "\"Horas/empleado\" es el tiempo de trabajo que se imputa a cada uno (duración total ÷ nº de empleados).")
                        .setFontSize(8).setFontColor(ColorConstants.GRAY).setMarginTop(15));
            }

            documento.close();
        }

        return salida.toByteArray();
    }

    private double calcularDuracionHoras(Servicio servicio) {
        return (servicio.getHoraFin().toSecondOfDay() - servicio.getHoraInicio().toSecondOfDay()) / 3600.0;
    }

    private double calcularHorasPorEmpleado(Servicio servicio) {
    return (servicio.getHoraFin().toSecondOfDay() - servicio.getHoraInicio().toSecondOfDay()) / 3600.0;
}

    private Cell celdaCabecera(String texto) {
        return new Cell().add(new Paragraph(texto).setBold())
                .setBackgroundColor(new DeviceRgb(20, 184, 166))
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER);
    }
}