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

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static org.example.manager.ExcelManager.addTableRow;
import static org.example.manager.UtilsManager.formatearCosto;

public class PrintManager {
    private static final String USER_HOME = System.getProperty("user.home");


    private static final String PDF_FILE_PATH = USER_HOME + "\\Downloads\\Parqueadero\\facturas\\factura_parqueadero_";

    static void abrirPDF(String pdfFilePath) {
        try {
            File pdfFile = new File(pdfFilePath);
            if (pdfFile.exists()) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + pdfFilePath);
            } else {
                System.out.println("No se pudo abrir el archivo PDF: " + pdfFilePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void abrirArchivoPDF(String filePath) {
        try {
            // Crear un objeto File para el archivo PDF
            File pdfFile = new File(filePath);

            // Abrir el archivo PDF usando el programa predeterminado del sistema
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (pdfFile.exists()) {
                    desktop.open(pdfFile);
                } else {
                    System.out.println("El archivo PDF no existe.");
                }
            } else {
                System.out.println("El sistema no soporta la operación de abrir archivos.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String generarPDF(String placa, String fechaHoraEntrada, String fechaHoraSalida, String tiempoTranscurrido, int costo) {
        String pdfFilePath = PDF_FILE_PATH + placa + "_" + fechaHoraSalida.replaceAll("[:\\-\\s]", "_") + ".pdf";
        try {
            // Define las dimensiones del papel térmico
            float anchoMm = 80; // ancho en mm
            float altoMm = 90; // alto en mm
            float anchoPuntos = anchoMm * 2.83465f; // Convertir mm a puntos
            float altoPuntos = altoMm * 2.83465f; // Convertir mm a puntos

            PageSize pageSize = new PageSize(anchoPuntos, altoPuntos);

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
                    .setFontSize(8)); // Ajustar tamaño de fuente para papel térmico

            // Crear la tabla para detalles
            Table table = new Table(new float[]{2, 3}); // Dos columnas: la primera más estrecha, la segunda más ancha
            table.setWidth(UnitValue.createPercentValue(100)); // Ancho de la tabla al 100%

            // Agregar las filas a la tabla
            addTableRow(table, "PLACA:", placa.toUpperCase());
            addTableRow(table, "HORA ENTRADA:", fechaHoraEntrada);
            addTableRow(table, "HORA SALIDA:", fechaHoraSalida);
            addTableRow(table, "TIEMPO:", tiempoTranscurrido);
            addTableRow(table, "TOTAL:", formatearCosto(costo));

            document.add(table);

            // Otra línea separadora
            document.add(new Paragraph(new String(new char[48]).replace("\0", "_"))
                    .setFont(fontNormal)
                    .setFontSize(8)); // Ajustar tamaño de fuente para papel térmico

            document.add(new Paragraph("¡Gracias por su visita!")
                    .setFont(fontNormal)
                    .setFontSize(8) // Ajustar tamaño de fuente para papel térmico
                    .setTextAlignment(TextAlignment.CENTER));

            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pdfFilePath;
    }
}
