package games.sparking.altara.punishment;

import lombok.Getter;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

/**
 * The violation category that triggered a punishment.
 * Each infraction carries contextual description text, a representative {@link Material}
 * for menu icons, and a set of recommended {@link RestrictionAction}s used by the
 * in-game punishment builder.
 */
@Getter
public enum InfractionType {

    PROFANITY("Profanity",
            List.of("swear", "cuss", "cursing", "curse"),
            "Use of profane, vulgar, or offensive language is not permitted.",
            "I acknowledge the use of inappropriate language and will keep my communication respectful.",
            Material.PAPER, false),

    ADVERTISING("Unauthorized Advertising",
            List.of("ads", "advertise", "server links"),
            "Promoting external servers, websites, or services is strictly prohibited.",
            "I understand that unsolicited advertising is against the rules.",
            Material.FIREWORK_ROCKET, false),

    SPAM("Spam & Message Flooding",
            List.of("spam", "flooding"),
            "Sending repetitive, irrelevant, or excessive messages is not allowed.",
            "I acknowledge that my messages were disruptive and will communicate responsibly.",
            Material.BOOK, false),

    DISRUPTION("Community Disruption",
            List.of("troll", "trolling", "roleplay", "rp", "public rp"),
            "Behaviour that intentionally disrupts the community experience is not allowed.",
            "I accept that my behaviour was disruptive and will contribute positively.",
            Material.LAVA_BUCKET, false),

    DISRESPECT("Disrespectful Behaviour",
            List.of("disrespect", "rude"),
            "All players must be treated with respect. Harassment and insults are prohibited.",
            "I understand that my behaviour was inappropriate and will treat others respectfully.",
            Material.FEATHER, false),

    HARASSMENT("Bullying & Harassment",
            List.of("bully", "harass"),
            "Targeting individuals with repeated negative behaviour is strictly prohibited.",
            "I acknowledge that my actions were harassing and will not engage in such conduct again.",
            Material.IRON_SWORD, false),

    HATE_SPEECH("Hate Speech & Discrimination",
            List.of("racism", "racist", "homophobia", "slurs"),
            "Speech that discriminates based on race, ethnicity, gender, sexuality, or religion is not tolerated.",
            "I accept that my speech was discriminatory and will uphold a welcoming environment.",
            Material.BARRIER, false),

    INAPPROPRIATE_CONTENT("Inappropriate Content",
            List.of("nudity", "nude skins", "NSFW", "inappropriate builds"),
            "Skins, builds, or speech that are sexually explicit or otherwise inappropriate are prohibited.",
            "I acknowledge that my content was inappropriate and will comply with content guidelines.",
            Material.PINK_STAINED_GLASS, false),

    UNAUTHORIZED_MODS("Unauthorized Modifications",
            List.of("mods", "hacks", "cheats"),
            "The use of unauthorized client modifications, hacks, or cheats is not allowed.",
            "I confirm I was using an unauthorized client and have removed it.",
            Material.COMMAND_BLOCK, false),

    EXPLOITING("Abusing Game Exploits",
            List.of("exploit", "glitch", "dupe"),
            "Exploiting bugs or unintended mechanics for personal gain is not allowed.",
            "I acknowledge the use of exploits and will report issues rather than abuse them.",
            Material.END_CRYSTAL, false),

    META_GAMING("Meta Gaming",
            List.of("meta gaming", "meta", "ghosting", "ghost"),
            "Using out-of-character knowledge to influence in-character gameplay is prohibited.",
            "I acknowledge that Meta Gaming negatively impacts fair play and agree to avoid it.",
            Material.SPECTRAL_ARROW, false),

    FALSE_REPORTS("Abuse of Reporting System",
            List.of("false report", "fake report"),
            "Filing false, malicious, or misleading reports wastes staff time and is prohibited.",
            "I understand that false reporting is a serious offence.",
            Material.WRITABLE_BOOK, false),

    MINI_MODDING("Unauthorized Moderation",
            List.of("mini-mod", "self moderation"),
            "Players are not permitted to act in a staff role or moderate others.",
            "I acknowledge that I overstepped my role and will report rather than police others.",
            Material.LEAD, false),

    FOREIGN_LANGUAGE("Non-English Chat",
            List.of("foreign", "language", "non-english"),
            "Public chat must remain in English to ensure inclusive communication.",
            "I understand that public chat must be in English.",
            Material.GLOBE_BANNER_PATTERN, false),

    COMBAT_LOGGING("Combat Logging",
            List.of("combat log", "disconnect during fight"),
            "Disconnecting mid-combat to avoid in-game consequences is unfair and against the rules.",
            "I acknowledge logging out during combat and will remain in game during fights.",
            Material.TOTEM_OF_UNDYING, false),

    POLITICAL_CONTENT("Political Discussion",
            List.of("politics", "political talk"),
            "Political discussions are not permitted in public areas to maintain a neutral space.",
            "I accept that political discussion is not appropriate on the server.",
            Material.WRITTEN_BOOK, false),

    STAFF_IMPERSONATION("Impersonating Staff",
            List.of("impersonation", "fake staff"),
            "Pretending to be a staff member is strictly prohibited.",
            "I acknowledge that impersonating staff is a violation and will not misrepresent myself.",
            Material.NAME_TAG, false),

    TEMP_AUTOMATED("Automated Action – Pending Review",
            List.of("auto", "flagged", "temp action", "auto mod", "automated system"),
            "This action was taken automatically by our moderation systems. A staff member will review it.",
            "I understand this action was automated and will await staff review.",
            Material.REDSTONE_TORCH, true);

    // ── Fields ─────────────────────────────────────────────────────────────────

    private final String displayName;
    private final List<String> aliases;
    private final String description;
    private final String affirmation;
    private final Material material;
    /** Hidden infractions are not shown in the in-game punishment builder. */
    private final boolean hidden;

    InfractionType(String displayName, List<String> aliases,
                   String description, String affirmation,
                   Material material, boolean hidden) {
        this.displayName = displayName;
        this.aliases     = aliases;
        this.description = description;
        this.affirmation = affirmation;
        this.material    = material;
        this.hidden      = hidden;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Returns all non-hidden infraction types, used to populate the punishment menu. */
    public static InfractionType[] visibleValues() {
        return Arrays.stream(values())
                .filter(t -> !t.isHidden())
                .toArray(InfractionType[]::new);
    }

    /**
     * Returns recommended actions for a first-time / clean-account offence.
     * Delegates to {@link #getRecommendedActions(AccountStatus)} with {@link AccountStatus#CLEAN}.
     */
    public List<RestrictionAction> getRecommendedActions() {
        return getRecommendedActions(AccountStatus.CLEAN);
    }

    /**
     * Returns the default set of {@link RestrictionAction}s for this infraction type,
     * scaled to the player's current {@link AccountStatus}.
     *
     * <p>Progressive discipline tiers:
     * <ul>
     *   <li>{@link AccountLevel#LOW}    – first / isolated offence, baseline sanction</li>
     *   <li>{@link AccountLevel#MEDIUM} – repeat offender, noticeably longer sanction</li>
     *   <li>{@link AccountLevel#HIGH}   – pattern of behaviour, maximum recommended sanction</li>
     * </ul>
     *
     * <p>The unified {@link AccountStatus#getLevel()} covers all sanction types —
     * <p>All infractions scale on the single unified {@link AccountStatus#getLevel()}.
     * Warnings are never stacked on top of a real restriction.
     *
     * @param status the player's computed account status (never {@code null})
     */
    public List<RestrictionAction> getRecommendedActions(AccountStatus status) {
        AccountLevel level = status.getLevel();

        return switch (this) {

            // ── Minor chat offences ────────────────────────────────────────────
            // SPAM: 30 min → 3 h → 12 h
            case SPAM -> List.of(RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,
                    level == AccountLevel.HIGH   ? 12 * 60 * 60 * 1000L
                    : level == AccountLevel.MEDIUM ?  3 * 60 * 60 * 1000L
                    :                               30 * 60 * 1000L));

            // FOREIGN_LANGUAGE / MINI_MODDING: 30 min → 2 h → 6 h
            case FOREIGN_LANGUAGE, MINI_MODDING -> List.of(RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,
                    level == AccountLevel.HIGH   ?  6 * 60 * 60 * 1000L
                    : level == AccountLevel.MEDIUM ?  2 * 60 * 60 * 1000L
                    :                               30 * 60 * 1000L));

            // PROFANITY: 1 h → 6 h → 24 h
            case PROFANITY -> List.of(RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,
                    level == AccountLevel.HIGH   ? 24 * 60 * 60 * 1000L
                    : level == AccountLevel.MEDIUM ?  6 * 60 * 60 * 1000L
                    :                                1 * 60 * 60 * 1000L));

            // DISRESPECT / POLITICAL_CONTENT: 2 h → 12 h → 3 days
            case DISRESPECT, POLITICAL_CONTENT -> List.of(RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,
                    level == AccountLevel.HIGH   ?  3L * 24 * 60 * 60 * 1000L
                    : level == AccountLevel.MEDIUM ? 12 * 60 * 60 * 1000L
                    :                                2 * 60 * 60 * 1000L));

            // ── Moderate offences ──────────────────────────────────────────────
            // ADVERTISING: 12 h mute → 3-day mute + 12 h ban → 7-day mute + 3-day ban
            case ADVERTISING -> level == AccountLevel.HIGH
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 7L  * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,       3L  * 24 * 60 * 60 * 1000L))
                    : level == AccountLevel.MEDIUM
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 3L  * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,       12  * 60 * 60 * 1000L))
                    : List.of(
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 12  * 60 * 60 * 1000L));

            // DISRUPTION: 6 h mute + 3 h ban → 24 h mute + 12 h ban → 3-day mute + 3-day ban
            case DISRUPTION -> level == AccountLevel.HIGH
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 3L  * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,       3L  * 24 * 60 * 60 * 1000L))
                    : level == AccountLevel.MEDIUM
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 24  * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,       12  * 60 * 60 * 1000L))
                    : List.of(
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,  6  * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,         3  * 60 * 60 * 1000L));

            // ── Reporting abuse ────────────────────────────────────────────────
            // FALSE_REPORTS: 30-day report + 6 h mute → 90-day report + 24 h mute → permanent report + 3-day ban
            case FALSE_REPORTS -> level == AccountLevel.HIGH
                    ? List.of(
                            RestrictionAction.permanent(PunishmentType.REPORT),
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,       3L  * 24 * 60 * 60 * 1000L))
                    : level == AccountLevel.MEDIUM
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.REPORT,           90L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 24  * 60 * 60 * 1000L))
                    : List.of(
                            RestrictionAction.temporary(PunishmentType.REPORT,           30L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,  6  * 60 * 60 * 1000L));

            // ── Serious behaviour offences ─────────────────────────────────────
            // HARASSMENT: 1-day ban + 3-day mute → 3-day ban + 7-day mute → 7-day ban + 14-day mute
            case HARASSMENT -> level == AccountLevel.HIGH
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,        7L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 14L * 24 * 60 * 60 * 1000L))
                    : level == AccountLevel.MEDIUM
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,        3L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,  7L * 24 * 60 * 60 * 1000L))
                    : List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,        1L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,  3L * 24 * 60 * 60 * 1000L));

            // INAPPROPRIATE_CONTENT: 3-day ban + 7-day mute → 7-day ban + 14-day mute → 14-day ban + 30-day mute
            case INAPPROPRIATE_CONTENT -> level == AccountLevel.HIGH
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,        14L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,  30L * 24 * 60 * 60 * 1000L))
                    : level == AccountLevel.MEDIUM
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,         7L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,  14L * 24 * 60 * 60 * 1000L))
                    : List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,         3L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,   7L * 24 * 60 * 60 * 1000L));

            // ── Zero-tolerance offences ────────────────────────────────────────
            // HATE_SPEECH: 7-day ban + 30-day mute + 7-day DC → 14-day ban + 60-day mute + 14-day DC → permanent all
            case HATE_SPEECH -> level == AccountLevel.HIGH
                    ? List.of(
                            RestrictionAction.permanent(PunishmentType.SUSPENSION),
                            RestrictionAction.permanent(PunishmentType.CHAT_RESTRICTION),
                            RestrictionAction.permanent(PunishmentType.DISCORD_RESTRICTION))
                    : level == AccountLevel.MEDIUM
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,          14L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,    60L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.DISCORD_RESTRICTION, 14L * 24 * 60 * 60 * 1000L))
                    : List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,           7L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,    30L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.DISCORD_RESTRICTION,  7L * 24 * 60 * 60 * 1000L));

            // STAFF_IMPERSONATION: 7-day ban + 14-day mute + 7-day DC → 14-day ban + 30-day mute + 14-day DC → 30-day ban + 60-day mute + permanent DC
            case STAFF_IMPERSONATION -> level == AccountLevel.HIGH
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,          30L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,    60L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.permanent(PunishmentType.DISCORD_RESTRICTION))
                    : level == AccountLevel.MEDIUM
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,          14L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,    30L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.DISCORD_RESTRICTION, 14L * 24 * 60 * 60 * 1000L))
                    : List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,           7L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION,    14L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.DISCORD_RESTRICTION,  7L * 24 * 60 * 60 * 1000L));

            // ── Competitive / game-integrity offences ──────────────────────────
            // COMBAT_LOGGING: 1-day ban + 3-day comp → 3-day ban + 7-day comp → 7-day ban + 14-day comp
            case COMBAT_LOGGING -> level == AccountLevel.HIGH
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,    7L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.COMP_GAMEPLAY,14L * 24 * 60 * 60 * 1000L))
                    : level == AccountLevel.MEDIUM
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,   3L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.COMP_GAMEPLAY, 7L * 24 * 60 * 60 * 1000L))
                    : List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,   1L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.COMP_GAMEPLAY, 3L * 24 * 60 * 60 * 1000L));

            // META_GAMING: 1-day ban + 3-day comp → 3-day ban + 7-day comp → 7-day ban + 14-day comp
            case META_GAMING -> level == AccountLevel.HIGH
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,    7L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.COMP_GAMEPLAY, 14L * 24 * 60 * 60 * 1000L))
                    : level == AccountLevel.MEDIUM
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,    3L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.COMP_GAMEPLAY,  7L * 24 * 60 * 60 * 1000L))
                    : List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,    1L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.COMP_GAMEPLAY,  3L * 24 * 60 * 60 * 1000L));

            // EXPLOITING: 7-day ban + 14-day comp → 14-day ban + 30-day comp → 30-day ban + 90-day comp
            case EXPLOITING -> level == AccountLevel.HIGH
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,    30L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.COMP_GAMEPLAY, 90L * 24 * 60 * 60 * 1000L))
                    : level == AccountLevel.MEDIUM
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,    14L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.COMP_GAMEPLAY, 30L * 24 * 60 * 60 * 1000L))
                    : List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,     7L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.COMP_GAMEPLAY, 14L * 24 * 60 * 60 * 1000L));

            // UNAUTHORIZED_MODS (hacking): 7-day ban + 30-day comp → 14-day ban + 90-day comp → 30-day ban + permanent comp
            case UNAUTHORIZED_MODS -> level == AccountLevel.HIGH
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,    30L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.permanent(PunishmentType.COMP_GAMEPLAY))
                    : level == AccountLevel.MEDIUM
                    ? List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,    14L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.COMP_GAMEPLAY, 90L * 24 * 60 * 60 * 1000L))
                    : List.of(
                            RestrictionAction.temporary(PunishmentType.SUSPENSION,     7L * 24 * 60 * 60 * 1000L),
                            RestrictionAction.temporary(PunishmentType.COMP_GAMEPLAY, 30L * 24 * 60 * 60 * 1000L));

            // ── Automated / pending-review placeholder ─────────────────────────
            case TEMP_AUTOMATED ->
                    List.of(RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 24 * 60 * 60 * 1000L));
        };
    }
}
