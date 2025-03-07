package oracleai.aiholo;

import java.time.*;
import java.util.Map;
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
            Map.entry(0, "em ponto"), Map.entry(15, "e quinze"), Map.entry(30, "e meia"), Map.entry(45, "para as"),
            Map.entry(1, "e um"), Map.entry(2, "e dois"), Map.entry(3, "e três"), Map.entry(4, "e quatro"),
            Map.entry(5, "e cinco"), Map.entry(6, "e seis"), Map.entry(7, "e sete"), Map.entry(8, "e oito"),
            Map.entry(9, "e nove"), Map.entry(10, "e dez"), Map.entry(11, "e onze"), Map.entry(12, "e doze"),
            Map.entry(13, "e treze"), Map.entry(14, "e quatorze"), Map.entry(16, "e dezesseis"),
            Map.entry(17, "e dezessete"), Map.entry(18, "e dezoito"), Map.entry(19, "e dezenove"),
            Map.entry(20, "e vinte"), Map.entry(21, "e vinte e um"), Map.entry(22, "e vinte e dois"),
            Map.entry(23, "e vinte e três"), Map.entry(24, "e vinte e quatro"), Map.entry(25, "e vinte e cinco"),
            Map.entry(26, "e vinte e seis"), Map.entry(27, "e vinte e sete"), Map.entry(28, "e vinte e oito"),
            Map.entry(29, "e vinte e nove"), Map.entry(40, "e quarenta"), Map.entry(41, "e quarenta e um"),
            Map.entry(42, "e quarenta e dois"), Map.entry(43, "e quarenta e três"), Map.entry(44, "e quarenta e quatro"),
            Map.entry(46, "e quarenta e seis"), Map.entry(47, "e quarenta e sete"), Map.entry(48, "e quarenta e oito"),
            Map.entry(49, "e quarenta e nove"), Map.entry(50, "e cinquenta"), Map.entry(51, "e cinquenta e um"),
            Map.entry(52, "e cinquenta e dois"), Map.entry(53, "e cinquenta e três"), Map.entry(54, "e cinquenta e quatro"),
            Map.entry(55, "e cinquenta e cinco"), Map.entry(56, "e cinquenta e seis"), Map.entry(57, "e cinquenta e sete"),
            Map.entry(58, "e cinquenta e oito"), Map.entry(59, "e cinquenta e nove")
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
        String minuteWord = minutes.getOrDefault(minute, "e " + minute);
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
