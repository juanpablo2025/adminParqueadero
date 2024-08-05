package org.example;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
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

import java.awt.*;
import java.io.*;
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
    private static final String EXCEL_FILE_PATH = USER_HOME + "\\Downloads\\Parqueadero\\parqueadero.xlsx";
    private static final String PDF_FILE_PATH = USER_HOME + "\\Downloads\\Parqueadero\\facturas\\factura_parqueadero_";
    private static final String MENSUALIDADES_FILE_PATH = USER_HOME + "\\Downloads\\Parqueadero\\mensualidades.xlsx";


    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        File excelFile = new File(EXCEL_FILE_PATH);
        if (!excelFile.exists()) {
            crearArchivoExcel(EXCEL_FILE_PATH, "Registro Parqueadero", new String[]{"Placa", "Fecha y Hora de Entrada", "Fecha y Hora de Salida","posicion cascos", "valor", "realizo"});
        }

        File mensualidadesFile = new File(MENSUALIDADES_FILE_PATH);
        if (!mensualidadesFile.exists()) {
            crearArchivoExcel(MENSUALIDADES_FILE_PATH, "Registro Mensualidades", new String[]{"Placa", "Fecha de Pago", "Fecha de Vencimiento"});
        }

        while (true) {
            mostrarMenu();
            int opcion = leerOpcion(scanner);

            switch (opcion) {
                case 1:
                    registrarEntradaMenu(scanner);
                    break;
                case 2:
                    registrarSalidaMenu(scanner);
                    break;
                case 3:
                    pagarMensualidadMenu(scanner);
                    break;
                case 4:
                    System.out.println("Saliendo del programa...");
                    renombrarArchivoConFecha(EXCEL_FILE_PATH);
                    return;
                default:
                    System.out.println("Opción no válida. Por favor, intente de nuevo.");
            }
        }
    }
    private static int leerOpcion(Scanner scanner) {
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
        return opcion;
    }

    private static void mostrarMenu() {
        System.out.println("\n--- Menú del Parqueadero ---");
        System.out.println("1. Registrar entrada de moto");
        System.out.println("2. Registrar salida de moto");
        System.out.println("3. Pagar mensualidad");
        System.out.println("4. Liquidar caja y salir del programa");
        System.out.print("Seleccione una opción: ");
    }


    private static void registrarEntradaMenu(Scanner scanner) {
        while (true) {
            System.out.println("Ingrese la placa de la moto (o escriba 'volver' para regresar al menú principal): ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("volver")) {
                mostrarMenuPrincipal(scanner); // Implementa esta función para mostrar el menú principal
                return; // Salir del método actual
            }

            if (input.isEmpty()) {
                System.out.println("Placa no ingresada. Por favor ingrese una placa o escriba 'volver' para regresar.");
                continue;
            }

            String placa = input;
            String fechaHoraEntrada = obtenerFechaHoraActual();
            String placaConSufijo = obtenerPlacaConSufijo(placa);

            // Nuevo menú para ingresar la posición de los cascos
            System.out.println("Ingrese la posición de los cascos: ");
            String posicionCascos = scanner.nextLine();

            registrarEntrada(placaConSufijo, fechaHoraEntrada, posicionCascos);

            // Guardar la posición de los cascos en un archivo
            guardarPosicionCascos(placaConSufijo, posicionCascos);
            generarPDFRegistro(placaConSufijo, fechaHoraEntrada, posicionCascos);

            System.out.println("Moto registrada con placa: " + placaConSufijo + ", fecha y hora de entrada: " + fechaHoraEntrada +
                    " y posición de los cascos: " + posicionCascos);

            // Opcional: Preguntar si desea registrar otra moto
            System.out.println("¿Desea registrar otra moto? (si/no)");
            String respuesta = scanner.nextLine();
            if (!respuesta.equalsIgnoreCase("si")) {
                return; // Salir del método actual
            }
        }
    }
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

    private static void abrirPDF(String pdfFilePath) {
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
    private static void guardarPosicionCascos(String placa, String posicionCascos) {
        try (FileInputStream fis = new FileInputStream(MENSUALIDADES_FILE_PATH)) {
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheet("Registro Mensualidades");

            // Si la hoja no existe, créala
            if (sheet == null) {
                sheet = workbook.createSheet("Registro Mensualidades");
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("Placa");
                headerRow.createCell(1).setCellValue("Posición Cascos");
            }

            // Agregar una nueva fila con la información
            int lastRowNum = sheet.getLastRowNum();
            Row row = sheet.createRow(lastRowNum + 1);
            row.createCell(0).setCellValue(placa);
            row.createCell(1).setCellValue(posicionCascos);

            try (FileOutputStream fos = new FileOutputStream(MENSUALIDADES_FILE_PATH)) {
                workbook.write(fos);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void mostrarMenuPrincipal(Scanner scanner) {
        mostrarMenu();
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
        Double valor = (double) costo;
        String tiempoTranscurrido = formatearTiempoTranscurrido(minutos);
        registrarSalida(placa, fechaHoraSalida, valor);

        String posicionCascos = leerPosicionCascos(placa);

        System.out.println("El costo del estacionamiento es: " + costo + " pesos");
        System.out.println("Posición de los cascos: " + posicionCascos);
        String pdfFilePath = generarPDF(placa, fechaHoraEntrada, fechaHoraSalida, tiempoTranscurrido, costo);

        // Preguntar si el usuario desea calcular el cambio
        System.out.println("¿Desea calcular el cambio a devolver? (si/no)");
        String respuesta = scanner.nextLine();

        if (respuesta.equalsIgnoreCase("si")) {
            // Calcular el cambio a devolver
            System.out.println("Ingrese el monto recibido del cliente: ");
            int montoRecibido = scanner.nextInt();
            scanner.nextLine();  // Consumir la nueva línea

            if (montoRecibido < costo) {
                System.out.println("El monto recibido es insuficiente. No se puede completar la transacción.");
            } else {
                int cambio = montoRecibido - costo;
                System.out.println("El cambio a devolver es: " + cambio + " pesos");
            }
        } else {
            System.out.println("No se calculará el cambio.");
        }

        // Preguntar si el usuario desea abrir el archivo PDF generado
        System.out.println("¿Desea abrir el archivo PDF generado? (si/no)");
        String abrirArchivo = scanner.nextLine();
        if (abrirArchivo.equalsIgnoreCase("si")) {
            abrirArchivoPDF(pdfFilePath);
        }
    }

    private static void abrirArchivoPDF(String filePath) {
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
    private static String leerPosicionCascos(String placa) {
        try (FileInputStream fis = new FileInputStream(MENSUALIDADES_FILE_PATH)) {
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheet("Registro Mensualidades");

            if (sheet == null) {
                return "No disponible";
            }

            for (Row row : sheet) {
                org.apache.poi.ss.usermodel.Cell placaCell = row.getCell(0);
                if (placaCell != null && placaCell.getStringCellValue().equals(placa)) {
                    org.apache.poi.ss.usermodel.Cell posicionCascosCell = row.getCell(1);
                    if (posicionCascosCell != null) {
                        return posicionCascosCell.getStringCellValue();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "No disponible";
    }

    private static void pagarMensualidadMenu(Scanner scanner) {
        System.out.println("Ingrese la placa de la moto para pagar la mensualidad: ");
        String placa = scanner.nextLine();
        pagarMensualidad(placa);
    }

    private static void crearArchivoExcel(String filePath, String sheetName, String[] headers) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            workbook.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void registrarEntrada(String placa, String fechaHoraEntrada, String posicionCascos) {
        try (FileInputStream fileInputStream = new FileInputStream(EXCEL_FILE_PATH);
             Workbook workbook = new XSSFWorkbook(fileInputStream);
             FileOutputStream outputStream = new FileOutputStream(EXCEL_FILE_PATH)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Asegúrate de que la primera fila tenga los encabezados correctos
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                headerRow = sheet.createRow(0);
            }
            if (headerRow.getCell(0) == null) {
                headerRow.createCell(0).setCellValue("Placa");
            }
            if (headerRow.getCell(1) == null) {
                headerRow.createCell(1).setCellValue("Fecha y Hora de Entrada");
            }
            if (headerRow.getCell(2) == null) {
                headerRow.createCell(2).setCellValue("Fecha y Hora de Salida");
            }
            if (headerRow.getCell(3) == null) {
                headerRow.createCell(3).setCellValue("Posición de los Cascos");
            }
            if (headerRow.getCell(4) == null) {
                headerRow.createCell(4).setCellValue("Valor");
            }

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
                row.createCell(2).setCellValue("");
                row.createCell(3).setCellValue(posicionCascos);
                row.createCell(4).setCellValue("0");
                // Inicialmente, la hora de salida está vacía

                workbook.write(outputStream);
                System.out.println("Moto registrada con placa: " + placa + " y fecha y hora de entrada: " + fechaHoraEntrada);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void registrarSalida(String placa, String fechaHoraSalida, double valor) {
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

                        // Establecer el valor en la columna "Valor"
                        org.apache.poi.ss.usermodel.Cell cellValor = row.getCell(4);
                        if (cellValor == null) {
                            cellValor = row.createCell(4);
                        }
                        cellValor.setCellValue(valor);

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

                                        // Establecer el valor en la columna "Valor"
                                        org.apache.poi.ss.usermodel.Cell cellValorAux = rowAux.getCell(4);
                                        if (cellValorAux == null) {
                                            cellValorAux = rowAux.createCell(4);
                                        }
                                        cellValorAux.setCellValue(valor);

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
                // Calcular el total acumulado
                double totalActual = 0;
                for (Row row : sheet) {
                    org.apache.poi.ss.usermodel.Cell cellValor = row.getCell(4);
                    if (cellValor != null && cellValor.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                        totalActual += cellValor.getNumericCellValue();
                    }
                }

                // Actualizar el total acumulado en F2
                Row totalRow = sheet.getRow(1);
                if (totalRow == null) {
                    totalRow = sheet.createRow(1);
                }
                org.apache.poi.ss.usermodel.Cell totalCell = totalRow.getCell(5);
                if (totalCell == null) {
                    totalCell = totalRow.createCell(5);
                }
                totalCell.setCellValue(totalActual);

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
        return LocalDateTime.now().format(formatter);
    }

    private static int calcularMinutos(String fechaHoraEntrada, String fechaHoraSalida) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
        LocalDateTime entrada = LocalDateTime.parse(fechaHoraEntrada, formatter);
        LocalDateTime salida = LocalDateTime.parse(fechaHoraSalida, formatter);
        Duration duration = Duration.between(entrada, salida);
        return (int) duration.toMinutes();
    }

    private static int calcularCosto(int minutos) {
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
    private static void pagarMensualidad(String placa) {
        String fechaPago = obtenerFechaHoraActual();
        LocalDateTime fechaVencimiento = LocalDateTime.now().plusMonths(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
        String fechaPagoFormatted = LocalDateTime.parse(fechaPago, formatter).format(formatter);
        String fechaVencimientoFormatted = fechaVencimiento.format(formatter);
        Double costoMensualidad = 40000.0;
        try (FileInputStream fileInputStream = new FileInputStream(MENSUALIDADES_FILE_PATH);
             Workbook workbook = new XSSFWorkbook(fileInputStream);
             FileOutputStream outputStream = new FileOutputStream(MENSUALIDADES_FILE_PATH)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean placaEncontrada = false;
            for (Row row : sheet) {
                org.apache.poi.ss.usermodel.Cell cellPlaca = row.getCell(0);
                if (cellPlaca != null && cellPlaca.getStringCellValue().equalsIgnoreCase(placa)) {
                    row.getCell(1).setCellValue(fechaPagoFormatted);
                    row.getCell(2).setCellValue(fechaVencimientoFormatted);
                    placaEncontrada = true;
                    break;
                }
            }

            if (!placaEncontrada) {
                int rowCount = sheet.getPhysicalNumberOfRows();
                Row row = sheet.createRow(rowCount);
                row.createCell(0).setCellValue(placa);
                row.createCell(1).setCellValue(fechaPagoFormatted);
                row.createCell(2).setCellValue(fechaVencimientoFormatted);
            }

            workbook.write(outputStream);
            System.out.println("Mensualidad registrada para la placa: " + placa);
            generarReciboMensualidad(placa, fechaPagoFormatted, fechaVencimientoFormatted, costoMensualidad);

        } catch (IOException e) {
            e.printStackTrace();
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

    private static void renombrarArchivoConFecha(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            String fechaActual = new SimpleDateFormat("ddMMyy").format(new Date());
            String nuevoNombre = "parqueadero" + fechaActual + ".xlsx";
            File nuevoArchivo = new File(file.getParent(), nuevoNombre);
            if (file.renameTo(nuevoArchivo)) {
                System.out.println("El archivo ha sido renombrado a: " + nuevoNombre);
            } else {
                System.out.println("No se pudo renombrar el archivo.");
            }
        } else {
            System.out.println("El archivo no existe.");
        }
    }

}
