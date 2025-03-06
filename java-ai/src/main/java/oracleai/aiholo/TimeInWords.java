package oracleai.aiholo;

import java.time.*;
import java.util.Map;

public class TimeInWords {

    private static final Map<Integer, String> NUMBERS_EN = Map.ofEntries(
            Map.entry(0, "Twelve"), Map.entry(1, "One"), Map.entry(2, "Two"), Map.entry(3, "Three"),
            Map.entry(4, "Four"), Map.entry(5, "Five"), Map.entry(6, "Six"), Map.entry(7, "Seven"),
            Map.entry(8, "Eight"), Map.entry(9, "Nine"), Map.entry(10, "Ten"), Map.entry(11, "Eleven"),
            Map.entry(12, "Twelve"), Map.entry(13, "One"), Map.entry(14, "Two"), Map.entry(15, "Three"),
            Map.entry(16, "Four"), Map.entry(17, "Five"), Map.entry(18, "Six"), Map.entry(19, "Seven"),
            Map.entry(20, "Eight"), Map.entry(21, "Nine"), Map.entry(22, "Ten"), Map.entry(23, "Eleven")
    );

    private static final Map<Integer, String> MINUTES_EN = Map.ofEntries(
            Map.entry(0, "o'clock"), Map.entry(15, "fifteen"), Map.entry(30, "thirty"), Map.entry(45, "forty-five")
    );

    private static final Map<String, String> AM_PM_EN = Map.of("AM", "AM", "PM", "PM");

    // Brazilian Portuguese mappings
    private static final Map<Integer, String> NUMBERS_PT = Map.ofEntries(
            Map.entry(0, "Doze"), Map.entry(1, "Uma"), Map.entry(2, "Duas"), Map.entry(3, "Três"),
            Map.entry(4, "Quatro"), Map.entry(5, "Cinco"), Map.entry(6, "Seis"), Map.entry(7, "Sete"),
            Map.entry(8, "Oito"), Map.entry(9, "Nove"), Map.entry(10, "Dez"), Map.entry(11, "Onze"),
            Map.entry(12, "Doze"), Map.entry(13, "Uma"), Map.entry(14, "Duas"), Map.entry(15, "Três"),
            Map.entry(16, "Quatro"), Map.entry(17, "Cinco"), Map.entry(18, "Seis"), Map.entry(19, "Sete"),
            Map.entry(20, "Oito"), Map.entry(21, "Nove"), Map.entry(22, "Dez"), Map.entry(23, "Onze")
    );

    private static final Map<Integer, String> MINUTES_PT = Map.ofEntries(
            Map.entry(0, "em ponto"), Map.entry(15, "e quinze"), Map.entry(30, "e meia"), Map.entry(45, "menos quinze")
    );

    private static final Map<String, String> AM_PM_PT = Map.of("AM", "da manhã", "PM", "da noite");

    public static String getTimeInWords(boolean inPortuguese) {
        ZoneId brazilZone = ZoneId.of("America/Sao_Paulo");
        LocalTime now = LocalTime.now(brazilZone);

        int hour = now.getHour() % 12;
        int minute = now.getMinute();
        boolean isAM = now.getHour() < 12;
        if (hour == 0) hour = 12;

        if (inPortuguese) {
            return formatTime(NUMBERS_PT, MINUTES_PT, AM_PM_PT, hour, minute, isAM);
        } else {
            return formatTime(NUMBERS_EN, MINUTES_EN, AM_PM_EN, hour, minute, isAM);
        }
    }

    private static String formatTime(Map<Integer, String> numbers, Map<Integer, String> minutes,
                                     Map<String, String> amPm, int hour, int minute, boolean isAM) {
        String hourWord = numbers.get(hour);
        String minuteWord = minutes.getOrDefault(minute, String.valueOf(minute));
        String amPmWord = isAM ? amPm.get("AM") : amPm.get("PM");

        if (minute == 0) {
            return hourWord + " " + minuteWord + " " + amPmWord;
        } else {
            return hourWord + " " + minuteWord + " " + amPmWord;
        }
    }

    public static void main(String[] args) {
        System.out.println("Current time in Brazil (English): " + getTimeInWords(false));
        System.out.println("Hora atual no Brasil (Português): " + getTimeInWords(true));
    }
}
