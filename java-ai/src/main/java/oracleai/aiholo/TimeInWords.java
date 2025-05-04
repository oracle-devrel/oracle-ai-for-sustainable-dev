package oracleai.aiholo;

import java.time.*;
import java.util.Map;

public class TimeInWords {

    private static final Map<String, ZoneId> TIME_ZONES = Map.of(
            "en-US", ZoneId.of("America/New_York"),   // US Eastern Time
            "en-GB", ZoneId.of("Europe/London"),      // UK Time
            "es-ES", ZoneId.of("Europe/Madrid"),      // Spain
            "pt-BR", ZoneId.of("America/Sao_Paulo"),  // Brazil
            "zh-SG", ZoneId.of("Asia/Singapore"),     // Singapore (Mandarin Chinese)
            "it-IT", ZoneId.of("Europe/Rome"),        // Italy
            "de-DE", ZoneId.of("Europe/Berlin"),      // Germany
            "hi-IN", ZoneId.of("Asia/Kolkata")        // India
    );

    private static final Map<Integer, String> NUMBERS_EN = Map.ofEntries(
            Map.entry(0, "Twelve"), Map.entry(1, "One"), Map.entry(2, "Two"), Map.entry(3, "Three"),
            Map.entry(4, "Four"), Map.entry(5, "Five"), Map.entry(6, "Six"), Map.entry(7, "Seven"),
            Map.entry(8, "Eight"), Map.entry(9, "Nine"), Map.entry(10, "Ten"), Map.entry(11, "Eleven")
    );

    private static final Map<Integer, String> NUMBERS_ZH = Map.ofEntries(
            Map.entry(0, "十二"), Map.entry(1, "一"), Map.entry(2, "二"), Map.entry(3, "三"),
            Map.entry(4, "四"), Map.entry(5, "五"), Map.entry(6, "六"), Map.entry(7, "七"),
            Map.entry(8, "八"), Map.entry(9, "九"), Map.entry(10, "十"), Map.entry(11, "十一")
    );

    private static final Map<Integer, String> MINUTES_COMMON = Map.ofEntries(
            Map.entry(0, "o'clock"), Map.entry(15, "fifteen"), Map.entry(30, "thirty"), Map.entry(45, "forty-five")
    );

    private static final Map<String, String> AM_PM_EN = Map.of("AM", "AM", "PM", "PM");
    private static final Map<String, String> AM_PM_ZH = Map.of("AM", "上午", "PM", "下午");

    public static String getTimeInWords(String languageCode) {
        ZoneId zone = TIME_ZONES.getOrDefault(languageCode, ZoneId.of("UTC"));
        LocalTime now = LocalTime.now(zone);

        int hour = now.getHour() % 12;
        int minute = now.getMinute();
        boolean isAM = now.getHour() < 12;
        if (hour == 0) hour = 12;

        return switch (languageCode) {
            case "pt-BR" -> formatTime(NUMBERS_EN, MINUTES_COMMON, AM_PM_EN, hour, minute, isAM);
            case "es-ES" -> formatTime(NUMBERS_EN, MINUTES_COMMON, AM_PM_EN, hour, minute, isAM);
            case "it-IT" -> formatTime(NUMBERS_EN, MINUTES_COMMON, AM_PM_EN, hour, minute, isAM);
            case "de-DE" -> formatTime(NUMBERS_EN, MINUTES_COMMON, AM_PM_EN, hour, minute, isAM);
            case "hi-IN" -> formatTime(NUMBERS_EN, MINUTES_COMMON, AM_PM_EN, hour, minute, isAM);
            case "zh-SG" -> formatTime(NUMBERS_ZH, MINUTES_COMMON, AM_PM_ZH, hour, minute, isAM);
            default -> formatTime(NUMBERS_EN, MINUTES_COMMON, AM_PM_EN, hour, minute, isAM);
        };
    }

    private static String formatTime(Map<Integer, String> numbers, Map<Integer, String> minutes,
                                     Map<String, String> amPm, int hour, int minute, boolean isAM) {
        String hourWord = numbers.get(hour);
        String minuteWord = minutes.getOrDefault(minute, String.valueOf(minute));
        String amPmWord = isAM ? amPm.get("AM") : amPm.get("PM");

        return hourWord + " " + minuteWord + " " + amPmWord;
    }

    public static void main(String[] args) {
        System.out.println("🇺🇸 English (US): " + getTimeInWords("en-US"));
        System.out.println("🇬🇧 English (GB): " + getTimeInWords("en-GB"));
        System.out.println("🇧🇷 Português (BR): " + getTimeInWords("pt-BR"));
        System.out.println("🇪🇸 Español (ES): " + getTimeInWords("es-ES"));
        System.out.println("🇮🇹 Italiano (IT): " + getTimeInWords("it-IT"));
        System.out.println("🇩🇪 Deutsch (DE): " + getTimeInWords("de-DE"));
        System.out.println("🇨🇳 中文 (SG): " + getTimeInWords("zh-SG")); // ✅ ADDED CHINESE (zh-SG)
        System.out.println("🇮🇳 हिन्दी (IN): " + getTimeInWords("hi-IN"));
    }
}