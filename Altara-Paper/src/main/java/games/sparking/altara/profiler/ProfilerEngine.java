package games.sparking.altara.profiler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import games.sparking.altara.Altara;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.punishment.Punishment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Scores a login for signs of a compromised account or ban evasion.
 *
 * <p>Being new is <b>not</b> suspicious on its own — every player starts with zero
 * playtime.  Points only come from things that are unusual for the account:
 * <ul>
 *   <li><b>Account takeover</b> — an established account logging in from an IP and
 *       network it has never used, especially after a long absence or with a new name.</li>
 *   <li><b>Ban evasion</b> — accounts sharing IPs with currently-banned accounts,
 *       weighted heavily when the joining account is brand new.</li>
 *   <li><b>Alt farming</b> — a fresh account on IPs where several other accounts
 *       were created in the last day.</li>
 * </ul>
 *
 * <p>Must run with the profile as it was <em>before</em> this login updates it (i.e. the
 * joining IP is not yet in {@code knownIps}), so it is evaluated during pre-login.
 * Performs blocking HTTP requests — never call on the main thread.
 */
public final class ProfilerEngine {

    private static final long HOUR = TimeUnit.HOURS.toMillis(1);
    private static final long DAY  = TimeUnit.DAYS.toMillis(1);

    /** Max linked accounts inspected for bans (each may cost a request). */
    private static final int MAX_ALTS_CHECKED = 25;

    private ProfilerEngine() {}

    @Getter
    @RequiredArgsConstructor
    public static final class Result {
        private final int score;
        private final List<String> reasons;
        private final int bannedAltCount;

        public boolean shouldFlag() {
            return score >= ProfilerService.FLAG_THRESHOLD;
        }
    }

    public static Result evaluate(Profile profile, String loginName, String ip) {
        long now = System.currentTimeMillis();
        int score = 0;
        List<String> reasons = new ArrayList<>();

        long ageMs      = now - profile.getFirstLogin();
        long playTime   = profile.getPlayTime();
        boolean fresh   = ageMs < DAY && playTime < HOUR;
        boolean established = playTime >= 2 * HOUR || (ageMs >= 14 * DAY && playTime >= HOUR / 2);

        // ── Account takeover signals ──────────────────────────────────────────
        List<String> knownIps = profile.getKnownIps();
        boolean newIp = !knownIps.isEmpty() && !knownIps.contains(ip);

        if (established && newIp) {
            score += 40;
            reasons.add("Established account logged in from a new IP");

            String network = networkOf(ip);
            boolean knownNetwork = network != null
                    && knownIps.stream().anyMatch(known -> network.equals(networkOf(known)));
            if (!knownNetwork) {
                score += 20;
                reasons.add("IP is on a network the account has never used");
            }

            long inactiveDays = (now - profile.getLastSeen()) / DAY;
            if (inactiveDays >= 60) {
                score += 40;
                reasons.add("Returned after " + inactiveDays + " days of inactivity");
            }

            if (profile.getName() != null && !profile.getName().equalsIgnoreCase(loginName)) {
                score += 20;
                reasons.add("Username changed since last login (" + profile.getName() + " → " + loginName + ")");
            }
        }

        // ── Linked accounts: ban evasion & alt farming ────────────────────────
        int bannedAlts = 0;
        int recentAlts = 0;
        List<JsonObject> alts = fetchAlts(profile.getUuid());
        for (int i = 0; i < alts.size(); i++) {
            JsonObject alt = alts.get(i);
            if (alt.has("firstLogin") && now - alt.get("firstLogin").getAsLong() < DAY) {
                recentAlts++;
            }

            if (i < MAX_ALTS_CHECKED) {
                UUID altUuid = UUID.fromString(alt.get("uuid").getAsString());
                List<Punishment> punishments = Altara.getSharedInstance().getPunishmentService().getPunishments(altUuid);
                if (punishments.stream().anyMatch(Punishment::isBan)) bannedAlts++;
            }
        }

        if (bannedAlts > 0) {
            score += Math.min(100, 40 + 20 * (bannedAlts - 1));
            reasons.add(bannedAlts + " linked account(s) currently banned");
            if (fresh) {
                score += 60;
                reasons.add("Brand-new account linked to a banned account");
            }
        }

        if (fresh && recentAlts >= 3) {
            score += recentAlts >= 6 ? 100 : 50;
            reasons.add(recentAlts + " other accounts created on shared IPs in the last 24h");
        }

        return new Result(score, reasons, bannedAlts);
    }

    /** Profiles sharing any known IP with this account (raw JSON, not cached). */
    private static List<JsonObject> fetchAlts(UUID uuid) {
        List<JsonObject> alts = new ArrayList<>();
        RequestResponse response = RequestHandler.get("api/profile/%s/alts", uuid.toString());
        if (!response.wasSuccessful()) return alts;

        for (JsonElement element : response.asArray()) {
            if (element.isJsonObject() && element.getAsJsonObject().has("uuid")) {
                alts.add(element.getAsJsonObject());
            }
        }
        return alts;
    }

    /**
     * Coarse network identifier: the /16 for IPv4, the /48 for IPv6.
     * Two IPs on the same network usually mean the same ISP/region.
     */
    static String networkOf(String ip) {
        if (ip == null) return null;
        if (ip.contains(":")) {
            String[] parts = ip.split(":");
            return parts.length >= 3 ? parts[0] + ":" + parts[1] + ":" + parts[2] : null;
        }
        String[] parts = ip.split("\\.");
        return parts.length == 4 ? parts[0] + "." + parts[1] : null;
    }
}
