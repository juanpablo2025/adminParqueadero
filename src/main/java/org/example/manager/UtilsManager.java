package org.example.manager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class UtilsManager {

    public static String obtenerFechaHoraActual() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
        return LocalDateTime.now().format(formatter);
    }


    static String formatearCosto(double costo) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###.00", symbols);
        return formatter.format(costo);
    }

    public static String formatearTiempoTranscurrido(int minutos) {
        int horas = minutos / 60;
        int minutosRestantes = minutos % 60;
        return String.format("%d:%02d", horas, minutosRestantes);
    }

    static String obtenerFechaActual() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
}
