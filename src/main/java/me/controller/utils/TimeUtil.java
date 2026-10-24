package me.controller.utils;

public class TimeUtil {
    
    public static long parseTime(String input) {
        if (input == null || input.isEmpty()) {
            return -1;
        }
        
        input = input.toLowerCase().trim();
        long multiplier = 60;
        String numberPart = input;
        
        if (input.endsWith("s")) {
            multiplier = 1;
            numberPart = input.substring(0, input.length() - 1);
        } else if (input.endsWith("m")) {
            multiplier = 60;
            numberPart = input.substring(0, input.length() - 1);
        } else if (input.endsWith("h")) {
            multiplier = 3600;
            numberPart = input.substring(0, input.length() - 1);
        }
        
        try {
            long number = Long.parseLong(numberPart);
            if (number <= 0) return -1;
            return number * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    public static String formatTime(long seconds) {
        if (seconds <= 0) return "0s";
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (secs > 0 || sb.length() == 0) {
            sb.append(secs).append("s");
        }
        
        return sb.toString().trim();
    }
}
