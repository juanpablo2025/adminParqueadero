package org.example.manager;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.example.manager.FacturaManager.generarReciboMensualidad;
import static org.example.manager.UtilsManager.obtenerFechaActual;
import static org.example.manager.UtilsManager.obtenerFechaHoraActual;

public class ExcelManager {
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String EXCEL_FILE_PATH = USER_HOME + "\\Downloads\\Parqueadero\\parqueadero.xlsx";
    private static final String MENSUALIDADES_FILE_PATH = USER_HOME + "\\Downloads\\Parqueadero\\mensualidades.xlsx";

    public static void crearArchivoExcel(String filePath, String sheetName, String[] headers) {
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

    public static void registrarSalida(String placa, String fechaHoraSalida, double valor) {
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

    public static boolean placaYaRegistradaSalida(String placa) {
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

    public static String leerHoraEntrada(String placa) {
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

    static void addTableRow(Table table, String title, String value) throws IOException {
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

    public static void renombrarArchivoConFecha(String filePath) {
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

    public static void pagarMensualidad(String placa) {
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

    public static String leerPosicionCascos(String placa) {
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


    public static void registrarEntrada(String placa, String fechaHoraEntrada, String posicionCascos) {
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

    public static String obtenerPlacaConSufijo(String placa) {
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

    public static void guardarPosicionCascos(String placa, String posicionCascos) {
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
}
