package org.example.manager;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import static org.example.manager.ExcelManager.addTableRow;
import static org.example.manager.PrintManager.abrirPDF;
import static org.example.manager.UtilsManager.formatearCosto;

public class FacturaManager {

    private static final String USER_HOME = System.getProperty("user.home");


    private static final String PDF_FILE_PATH = USER_HOME + "\\Downloads\\Parqueadero\\facturas\\factura_parqueadero_";


    public static void generarPDFRegistro(String placa, String fechaHoraEntrada, String posicionCascos) {
        try {
            // Define las dimensiones del papel térmico
            float anchoMm = 80; // ancho en mm
            float altoMm = 80; // alto en mm (puedes ajustar si necesitas)
            float anchoPuntos = anchoMm * 2.83465f;
            float altoPuntos = altoMm * 2.83465f;

            PageSize pageSize = new PageSize(anchoPuntos, altoPuntos);

            String pdfFilePath = PDF_FILE_PATH + "Registro_" + placa + "_" + fechaHoraEntrada.replaceAll("[:\\-\\s]", "_") + ".pdf";
            File pdfFile = new File(pdfFilePath);
            pdfFile.getParentFile().mkdirs(); // Crear directorio si no existe

            PdfWriter writer = new PdfWriter(pdfFilePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, pageSize);

            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Ajustar márgenes
            document.setMargins(5, 5, 5, 5); // Margen superior, derecho, inferior, izquierdo en puntos

            // Título
            document.add(new Paragraph("Recibo de Registro de Moto")
                    .setFont(fontBold)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2)); // Ajustar el margen inferior

            document.add(new Paragraph("Dirección: CL 54/Caracas")
                    .setFont(fontNormal)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(1)); // Ajustar el margen inferior

            document.add(new Paragraph("Horario: 5:00 AM - 7:30 PM (Lunes a Viernes)" +
                    "                            5:00 AM - 6:00 PM (Sábados)")
                    .setFont(fontNormal)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5)); // Ajustar el margen inferior

            // Línea separadora
            document.add(new Paragraph(new String(new char[48]).replace("\0", "_"))
                    .setFont(fontNormal)
                    .setFontSize(8)
                    .setMarginBottom(10)); // Ajustar el margen inferior

            // Crear la tabla para detalles
            Table table = new Table(new float[]{2, 3}); // Dos columnas: la primera más estrecha, la segunda más ancha
            table.setWidth(UnitValue.createPercentValue(100)); // Ancho de la tabla al 100%

            // Agregar las filas a la tabla
            addTableRow(table, "PLACA:", placa.toUpperCase());
            addTableRow(table, "HORA ENTRADA:", fechaHoraEntrada);
            addTableRow(table, "SECTOR CASCOS:", posicionCascos);

            document.add(table);

            // Otra línea separadora
            document.add(new Paragraph(new String(new char[48]).replace("\0", "_"))
                    .setFont(fontNormal)
                    .setFontSize(8)
                    .setMarginTop(10) // Ajustar el margen superior
                    .setMarginBottom(10)); // Ajustar el margen inferior

            // Añadir "¡Bienvenido, gracias por tu visita!" y ajustar la página para que se adapte al contenido
            document.add(new Paragraph("¡Bienvenido, gracias por tu visita!")
                    .setFont(fontNormal)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5)); // Ajustar el margen superior

            document.close();
            abrirPDF(pdfFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int calcularMinutos(String fechaHoraEntrada, String fechaHoraSalida) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
        LocalDateTime entrada = LocalDateTime.parse(fechaHoraEntrada, formatter);
        LocalDateTime salida = LocalDateTime.parse(fechaHoraSalida, formatter);
        Duration duration = Duration.between(entrada, salida);
        return (int) duration.toMinutes();
    }

    public static int calcularCosto(int minutos) {
        if (minutos <= 2) {
            return 0; // Periodo de gracia de 2 minutos
        } else if (minutos <= 60) {
            return 1200; // Tarifa por 1 hora o menos
        } else if (minutos <= 75) {
            return 1800; // Tarifa por más de 1 hora hasta 1 hora 15 minutos
        } else if (minutos <= 120) {
            return 2400; // Tarifa por más de 1 hora 15 minutos hasta 2 horas
        } else if (minutos <= 135) {
            return 3000; // Tarifa por más de 2 horas hasta 2 horas 15 minutos
        } else if (minutos <= 180) {
            return 3600; // Tarifa por más de 2 horas 15 minutos hasta 3 horas
        } else if (minutos <= 195) {
            return 4200; // Tarifa por más de 3 horas hasta 3 horas 15 minutos
        } else if (minutos <= 240) {
            return 4800; // Tarifa por más de 3 horas 15 minutos hasta 4 horas
        } else {
            return 5000; // Tarifa máxima por más de 4 horas 15 minutos
        }
    }

    public static void generarReciboMensualidad(String placa, String fechaPago, String fechaVencimiento, Double costo) {
        try {
            // Define las dimensiones del papel térmico
            float anchoMm = 80; // ancho en mm
            float altoMm = 90; // alto en mm
            float anchoPuntos = anchoMm * 2.83465f;
            float altoPuntos = altoMm * 2.83465f;

            PageSize pageSize = new PageSize(anchoPuntos, altoPuntos);

            String pdfFilePath = PDF_FILE_PATH + "Mensualidad_" + placa + "_" + fechaPago.replaceAll("[:\\-\\s]", "_") + ".pdf";
            File pdfFile = new File(pdfFilePath);
            pdfFile.getParentFile().mkdirs(); // Crear directorio si no existe

            PdfWriter writer = new PdfWriter(pdfFilePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, pageSize);

            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Ajustar márgenes
            document.setMargins(5, 5, 5, 5); // Margen superior, derecho, inferior, izquierdo en puntos

            // Título
            document.add(new Paragraph("Recibo de Registro de Moto")
                    .setFont(fontBold)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(2)); // Ajustar el margen inferior

            document.add(new Paragraph("Dirección: CL 54/Caracas")
                    .setFont(fontNormal)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(1)); // Ajustar el margen inferior

            document.add(new Paragraph("Horario: 5:00 AM - 7:30 PM (Lunes a Viernes)" +
                    "                            5:00 AM - 6:00 PM (Sábados)")
                    .setFont(fontNormal)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5)); // Ajustar el margen inferior
            // Línea separadora
            document.add(new Paragraph(new String(new char[48]).replace("\0", "_"))
                    .setFont(fontNormal)
                    .setFontSize(8));

            // Crear la tabla para detalles
            Table table = new Table(new float[]{2, 3}); // Dos columnas: la primera más estrecha, la segunda más ancha
            table.setWidth(UnitValue.createPercentValue(100)); // Ancho de la tabla al 100%

            // Agregar las filas a la tabla
            addTableRow(table, "PLACA:", placa.toUpperCase());
            addTableRow(table, "FECHA PAGO:", fechaPago);
            addTableRow(table, "VENCIMIENTO:", fechaVencimiento);
            addTableRow(table, "TOTAL:", formatearCosto(costo));

            document.add(table);

            // Otra línea separadora
            document.add(new Paragraph(new String(new char[48]).replace("\0", "_"))
                    .setFont(fontNormal)
                    .setFontSize(8));

            // Añadir "¡Gracias por su pago!" y ajustar la página para que se adapte al contenido
            document.add(new Paragraph("¡Gracias por su pago!")
                    .setFont(fontNormal)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER));



            document.close();
            abrirPDF(pdfFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
