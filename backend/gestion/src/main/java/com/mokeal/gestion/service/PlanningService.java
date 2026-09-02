package com.mokeal.gestion.service;

import com.itextpdf.kernel.colors.ColorConstants;
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
import java.util.List;
import java.util.Locale;
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
                Table tabla = new Table(UnitValue.createPercentArray(new float[]{20, 30, 50}))
                        .useAllAvailableWidth();

                tabla.addHeaderCell(celdaCabecera("Hora"));
                tabla.addHeaderCell(celdaCabecera("Cliente"));
                tabla.addHeaderCell(celdaCabecera("Dirección"));

                for (Servicio servicio : servicios) {
                    tabla.addCell(new Cell().add(new Paragraph(
                            servicio.getHoraInicio() + " - " + servicio.getHoraFin())));
                    tabla.addCell(new Cell().add(new Paragraph(servicio.getCliente().getNombre())));
                    tabla.addCell(new Cell().add(new Paragraph(servicio.getDireccion())));
                }

                documento.add(tabla);
            }

            documento.close();
        }

        return salida.toByteArray();
    }

    private Cell celdaCabecera(String texto) {
        return new Cell().add(new Paragraph(texto).setBold())
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(20, 184, 166))
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER);
    }
}