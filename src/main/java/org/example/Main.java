package org.example;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.io.font.constants.StandardFonts;

import com.itextpdf.layout.property.UnitValue;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    private static final int PRECIO_POR_HORA = 1200;
    private static final int INCREMENTO_1H15M = 600;
    private static final int PRECIO_2HORAS = 2400;
    private static final int PRECIO_MAXIMO_5HORAS = 5000;

    private static final String USER_HOME = System.getProperty("user.home");
    private static final String EXCEL_FILE_PATH = USER_HOME + "\\Downloads\\parqueadero.xlsx";
    private static final String PDF_FILE_PATH = USER_HOME + "\\Downloads\\factura_parqueadero_";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        File excelFile = new File(EXCEL_FILE_PATH);
        if (!excelFile.exists()) {
            crearArchivoExcel();
        }

        while (true) {
            System.out.println("\n--- Menú del Parqueadero ---");
            System.out.println("1. Registrar entrada de moto");
            System.out.println("2. Registrar salida de moto");
            System.out.println("3. Salir");
            System.out.print("Seleccione una opción: ");

            int opcion = 0;
            boolean entradaValida = false;

            while (!entradaValida) {
                try {
                    opcion = scanner.nextInt();
                    scanner.nextLine();  // Consumir la nueva línea
                    entradaValida = true;
                } catch (InputMismatchException e) {
                    System.out.println("Entrada inválida. Por favor, ingrese un número entero.");
                    scanner.nextLine();  // Limpiar el buffer del scanner
                }
            }

            switch (opcion) {
                case 1:
                    registrarEntradaMenu(scanner);
                    break;
                case 2:
                    registrarSalidaMenu(scanner);
                    break;
                case 3:
                    System.out.println("Saliendo del programa...");
                    return;
                default:
                    System.out.println("Opción no válida. Por favor, intente de nuevo.");
            }
        }
    }

    private static void registrarEntradaMenu(Scanner scanner) {
        System.out.println("Ingrese la placa de la moto: ");
        String placa = scanner.nextLine();
        String fechaHoraEntrada = obtenerFechaHoraActual();
        String placaConSufijo = obtenerPlacaConSufijo(placa);
        registrarEntrada(placaConSufijo, fechaHoraEntrada);
        System.out.println("Moto registrada con placa: " + placaConSufijo + " y fecha y hora de entrada: " + fechaHoraEntrada);
    }

    private static void registrarSalidaMenu(Scanner scanner) {
        System.out.println("Ingrese la placa de la moto: ");
        String placa = scanner.nextLine();
        String fechaHoraSalida = obtenerFechaHoraActual();
        String fechaHoraEntrada = leerHoraEntrada(placa);

        if (fechaHoraEntrada == null) {
            System.out.println("No se encontró un registro de entrada para la placa: " + placa);
            return;
        }

        if (placaYaRegistradaSalida(placa)) {
            System.out.println("La placa ya tiene registrada una fecha y hora de salida.");
            return;
        }

        int minutos = calcularMinutos(fechaHoraEntrada, fechaHoraSalida);
        int costo = calcularCosto(minutos);
        String tiempoTranscurrido = formatearTiempoTranscurrido(minutos);
        registrarSalida(placa, fechaHoraSalida);

        System.out.println("El costo del estacionamiento es: " + (costo) + " pesos");
        generarPDF(placa, fechaHoraEntrada, fechaHoraSalida, tiempoTranscurrido, costo);
    }

    private static void crearArchivoExcel() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Registro Parqueadero");

        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Placa");
        headerRow.createCell(1).setCellValue("Fecha y Hora de Entrada");
        headerRow.createCell(2).setCellValue("Fecha y Hora de Salida");

        try (FileOutputStream outputStream = new FileOutputStream(EXCEL_FILE_PATH)) {
            workbook.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void registrarEntrada(String placa, String fechaHoraEntrada) {
        try (FileInputStream fileInputStream = new FileInputStream(EXCEL_FILE_PATH);
             Workbook workbook = new XSSFWorkbook(fileInputStream);
             FileOutputStream outputStream = new FileOutputStream(EXCEL_FILE_PATH)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean yaRegistrado = false;
            for (Row row : sheet) {
                org.apache.poi.ss.usermodel.Cell cellPlaca = row.getCell(0);
                if (cellPlaca != null && cellPlaca.getStringCellValue().equalsIgnoreCase(placa)) {
                    org.apache.poi.ss.usermodel.Cell cellHoraSalida = row.getCell(2);
                    if (cellHoraSalida == null || cellHoraSalida.getStringCellValue().isEmpty()) {
                        yaRegistrado = true;
                        System.out.println("La placa " + placa + " ya está ingresada y no ha salido.");
                        return;
                    }
                }
            }

            if (!yaRegistrado) {
                int rowCount = sheet.getPhysicalNumberOfRows();
                Row row = sheet.createRow(rowCount);
                row.createCell(0).setCellValue(placa);
                row.createCell(1).setCellValue(fechaHoraEntrada);
                row.createCell(2).setCellValue(""); // Inicialmente, la hora de salida está vacía

                workbook.write(outputStream);
                System.out.println("Moto registrada con placa: " + placa + " y fecha y hora de entrada: " + fechaHoraEntrada);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void registrarSalida(String placa, String fechaHoraSalida) {
        try (FileInputStream fileInputStream = new FileInputStream(EXCEL_FILE_PATH);
             Workbook workbook = new XSSFWorkbook(fileInputStream);
             FileOutputStream outputStream = new FileOutputStream(EXCEL_FILE_PATH)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean placaEncontrada = false;
            boolean salidaRegistrada = false;
            int identificador = 1;

            // Buscar la placa sin identificador primero
            for (Row row : sheet) {
                org.apache.poi.ss.usermodel.Cell cellPlaca = row.getCell(0);
                if (cellPlaca != null && cellPlaca.getStringCellValue().equalsIgnoreCase(placa)) {
                    org.apache.poi.ss.usermodel.Cell cellHoraSalida = row.getCell(2);
                    if (cellHoraSalida == null || cellHoraSalida.getStringCellValue().isEmpty()) {
                        // La placa no ha salido aún, actualice la salida directamente
                        cellHoraSalida = row.createCell(2);
                        cellHoraSalida.setCellValue(fechaHoraSalida);
                        placaEncontrada = true;
                        salidaRegistrada = true;
                        break;
                    } else {
                        // La placa ya ha salido, buscar variantes con identificador
                        placaEncontrada = true;
                        while (true) {
                            String placaConIdentificador = placa + "(" + identificador + ")";
                            boolean placaConIdentificadorEncontrada = false;

                            for (Row rowAux : sheet) {
                                org.apache.poi.ss.usermodel.Cell cellPlacaAux = rowAux.getCell(0);
                                if (cellPlacaAux != null && cellPlacaAux.getStringCellValue().equalsIgnoreCase(placaConIdentificador)) {
                                    org.apache.poi.ss.usermodel.Cell cellHoraSalidaAux = rowAux.getCell(2);
                                    if (cellHoraSalidaAux == null || cellHoraSalidaAux.getStringCellValue().isEmpty()) {
                                        // Actualizar la salida si se encuentra la placa con identificador y aún no ha salido
                                        cellHoraSalidaAux = rowAux.createCell(2);
                                        cellHoraSalidaAux.setCellValue(fechaHoraSalida);
                                        placaEncontrada = true;
                                        salidaRegistrada = true;
                                        break;
                                    } else {
                                        placaConIdentificadorEncontrada = true;
                                    }
                                }
                            }

                            if (salidaRegistrada) {
                                break;
                            }

                            if (!placaConIdentificadorEncontrada) {
                                // No se encontró un registro con el identificador actual
                                break;
                            }
                            identificador++;
                        }
                        break;
                    }
                }
            }

            if (!placaEncontrada) {
                System.out.println("No se encontró un registro de entrada para la placa: " + placa);
            } else if (!salidaRegistrada) {
                System.out.println("La placa ya ha registrado salida: " + placa);
            } else {
                workbook.write(outputStream);
                System.out.println("Registro de salida actualizado para la placa: " + placa);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean placaYaRegistradaSalida(String placa) {
        try (FileInputStream fileInputStream = new FileInputStream(EXCEL_FILE_PATH);
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                org.apache.poi.ss.usermodel.Cell cellPlaca = row.getCell(0);
                if (cellPlaca != null && cellPlaca.getStringCellValue().equalsIgnoreCase(placa)) {
                    org.apache.poi.ss.usermodel.Cell cellHoraSalida = row.getCell(2);
                    if (cellHoraSalida != null && !cellHoraSalida.getStringCellValue().isEmpty()) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String leerHoraEntrada(String placa) {
        try (FileInputStream fileInputStream = new FileInputStream(EXCEL_FILE_PATH);
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                org.apache.poi.ss.usermodel.Cell cellPlaca = row.getCell(0);
                if (cellPlaca != null && cellPlaca.getStringCellValue().equalsIgnoreCase(placa)) {
                    org.apache.poi.ss.usermodel.Cell cellHoraEntrada = row.getCell(1);
                    return cellHoraEntrada.getStringCellValue();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String obtenerFechaHoraActual() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
    }

    private static int calcularMinutos(String fechaHoraEntrada, String fechaHoraSalida) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime entrada = LocalDateTime.parse(fechaHoraEntrada, formatter);
        LocalDateTime salida = LocalDateTime.parse(fechaHoraSalida, formatter);

        return (int) Duration.between(entrada, salida).toMinutes();
    }

    private static int calcularCosto(int minutos) {
        if (minutos <= 60) {
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

    private static String formatearCosto(double costo) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###.00", symbols);
        return formatter.format(costo);
    }

    private static String formatearTiempoTranscurrido(int minutos) {
        int horas = minutos / 60;
        int minutosRestantes = minutos % 60;
        return String.format("%d:%02d", horas, minutosRestantes);
    }

    private static String obtenerPlacaConSufijo(String placa) {
        try (FileInputStream fileInputStream = new FileInputStream(EXCEL_FILE_PATH);
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            String placaConSufijo = placa;
            int sufijo = 1;

            for (Row row : sheet) {
                org.apache.poi.ss.usermodel.Cell cellPlaca = row.getCell(0);
                org.apache.poi.ss.usermodel.Cell cellFechaEntrada = row.getCell(1);

                if (cellPlaca != null && cellPlaca.getStringCellValue().startsWith(placa)) {
                    String fechaEntrada = cellFechaEntrada.getStringCellValue();
                    if (fechaEntrada.startsWith(obtenerFechaActual())) {
                        placaConSufijo = placa + "(" + sufijo + ")";
                        sufijo++;
                    }
                }
            }

            return placaConSufijo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return placa;
    }

    private static String obtenerFechaActual() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    public static void generarPDF(String placa, String fechaHoraEntrada, String fechaHoraSalida, String tiempoTranscurrido, int costo) {
        try {
            String pdfFilePath = PDF_FILE_PATH + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf";
            File pdfFile = new File(pdfFilePath);
            pdfFile.getParentFile().mkdirs(); // Crear directorio si no existe

            PdfWriter writer = new PdfWriter(pdfFilePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Título
            document.add(new Paragraph("Recibo de Parqueadero")
                    .setFont(fontBold)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Dirección: CL 54 / Caracas")
                    .setFont(fontNormal)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Horario: 8:00 AM - 7:30 PM (Lunes a Viernes), 5:00 AM - 6:00 PM (Sábados)")
                    .setFont(fontNormal)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));

            // Línea separadora
            document.add(new Paragraph(new String(new char[90]).replace("\0", "_"))
                    .setFont(fontNormal)
                    .setFontSize(10));

            // Crear la tabla para detalles
            Table table = new Table(new float[]{2, 3}); // Dos columnas: la primera más estrecha, la segunda más ancha
            table.setWidth(UnitValue.createPercentValue(100)); // Ancho de la tabla al 100%

            // Agregar las filas a la tabla
            addTableRow(table, "PLACA:", placa.toUpperCase());
            addTableRow(table, "FECHA Y HORA DE ENTRADA:", fechaHoraEntrada);
            addTableRow(table, "FECHA Y HORA DE SALIDA:", fechaHoraSalida);
            addTableRow(table, "TIEMPO TRANSCURRIDO:", tiempoTranscurrido);
            addTableRow(table, "TOTAL:", formatearCosto(costo));

            document.add(table);

            // Otra línea separadora
            document.add(new Paragraph(new String(new char[90]).replace("\0", "_"))
                    .setFont(fontNormal)
                    .setFontSize(10));

            document.add(new Paragraph("¡Gracias por su visita!")
                    .setFont(fontNormal)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));

            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addTableRow(Table table, String title, String value) throws IOException {
        Cell titleCell = new Cell().add(new Paragraph(title.toUpperCase())
                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                        .setFontSize(10))
                .setTextAlignment(TextAlignment.LEFT)
                .setBorder(Border.NO_BORDER); // Sin borde

        Cell valueCell = new Cell().add(new Paragraph(value + " ")
                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                        .setFontSize(10))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER); // Sin borde

        table.addCell(titleCell);
        table.addCell(valueCell);
    }
}
