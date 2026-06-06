package games.sparking.altara.punishment;

public final class PunishmentModifyValidator {

    public static final long MAX_DURATION_MILLIS = 365L * 24L * 60L * 60L * 1000L;
    public static final long MIN_DURATION_MILLIS = 60_000L;
    public static final int MAX_MESSAGE_LENGTH = 256;

    private PunishmentModifyValidator() {
    }

    public static String validateDuration(PunishmentType type, long durationMillis) {
        if (type == null) {
            return "Punishment type is required.";
        }

        if (type == PunishmentType.WARN) {
            if (durationMillis != 0L) {
                return "Warnings must use an immediate duration (0s).";
            }
            return null;
        }

        if (durationMillis == -1L) {
            return null;
        }

        if (durationMillis < MIN_DURATION_MILLIS) {
            return "Duration must be at least 1 minute.";
        }

        if (durationMillis > MAX_DURATION_MILLIS) {
            return "Duration cannot exceed 365 days.";
        }

        return null;
    }

    public static String validateReason(InfractionType reason) {
        return reason == null ? "Reason is required." : null;
    }

    public static String validateMessage(String message) {
        if (message != null && message.length() > MAX_MESSAGE_LENGTH) {
            return "Linked message cannot exceed " + MAX_MESSAGE_LENGTH + " characters.";
        }
        return null;
    }

    public static String normalizeMessage(String message) {
        if (message == null) {
            return null;
        }

        String trimmed = message.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static long toExpiresAtFromNow(PunishmentType type, long durationMillis) {
        if (type == PunishmentType.WARN) {
            return System.currentTimeMillis();
        }

        if (durationMillis == -1L) {
            return -1L;
        }

        long now = System.currentTimeMillis();
        try {
            return Math.addExact(now, durationMillis);
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }
}

