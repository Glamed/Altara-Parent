package games.sparking.altara.report;

/** Lifecycle of a {@link Report}. */
public enum ReportStatus {

    /** Awaiting a staff member; visible in the queue. */
    PENDING,

    /** Claimed by a staff member (in-game or via the website) and being worked. */
    IN_PROGRESS,

    /** Staff found probable cause and (usually) issued a punishment. */
    ACCEPTED,

    /** Staff found no probable cause. */
    REJECTED,

    /** The report itself was abusive/malicious. */
    ABUSIVE,

    /** No staff claimed it before it aged out. */
    EXPIRED;

    public boolean isTerminal() {
        return this != PENDING && this != IN_PROGRESS;
    }

    public static ReportStatus parse(String value, ReportStatus fallback) {
        if (value == null) return fallback;
        try {
            return ReportStatus.valueOf(value.replace('-', '_').toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
