package games.sparking.altara.punishment;


import lombok.Getter;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

@Getter
public enum InfractionType {

    PROFANITY("Profanity", List.of("swear", "cuss", "cursing", "curse"),
            "Use of profane, vulgar, or offensive language is not permitted. We maintains a family-friendly environment appropriate for all ages.",
            "I acknowledge the use of inappropriate language and will keep my communication respectful.",
            Material.PAPER, false),

    ADVERTISING("Unauthorized Advertising", List.of("ads", "advertise", "server links"),
            "Promoting external servers, websites, or services unrelated our services is strictly prohibited in any form.",
            "I understand that unsolicited advertising is against the rules and will not promote external content.",
            Material.FIREWORK_ROCKET, false),

    SPAM("Spam & Message Flooding", List.of("spam", "flooding"),
            "Sending repetitive, irrelevant, or excessive messages disrupts communication and is not allowed.",
            "I acknowledge that my messages were disruptive and will communicate responsibly.",
            Material.BOOK, false),

    DISRUPTION("Community Disruption", List.of("troll", "trolling", "roleplay", "rp", "public rp"),
            "Behavior that intentionally disrupts community experience—such as trolling, excessive roleplaying in global chat, or inciting unrest—is not allowed.",
            "I accept that my behavior was disruptive and will contribute positively to the community.",
            Material.LAVA_BUCKET, false),

    DISRESPECT("Disrespectful Behavior", List.of("disrespect", "rude"),
            "All players must be treated with respect. Harassment, insults, or offensive behavior are strictly prohibited.",
            "I understand that my behavior was inappropriate and will treat others respectfully.",
            Material.FEATHER, false),

    HARASSMENT("Bullying & Harassment", List.of("bully", "harass"),
            "Targeting individuals with repeated negative or hostile behavior is strictly prohibited and will result in disciplinary action.",
            "I acknowledge that my actions were harassing in nature and will not engage in such conduct again.",
            Material.IRON_SWORD, false),

    HATE_SPEECH("Hate Speech & Discrimination", List.of("racism", "racist", "homophobia", "slurs"),
            "Speech that discriminates based on race, ethnicity, gender, sexuality, religion, or identity is not tolerated.",
            "I accept that my speech was discriminatory and will uphold a welcoming environment for all players.",
            Material.BARRIER, false),

    INAPPROPRIATE_CONTENT("Inappropriate Content", List.of("nudity", "nude skins", "nsfw", "inappropriate builds"),
            "Skins, builds, usernames, or speech that are sexually explicit, suggestive, or otherwise inappropriate are strictly prohibited.",
            "I acknowledge that my content was inappropriate and will ensure compliance with content guidelines.",
            Material.PINK_STAINED_GLASS, false),

    UNAUTHORIZED_MODS("Unauthorized Modifications", List.of("mods", "hacks", "cheats"),
            "The use of unauthorized client modifications, hacks, or cheats is not allowed. Only approved clients may be used.",
            "I confirm I was using an unauthorized client and have removed it.",
            Material.COMMAND_BLOCK, false),

    EXPLOITING("Abusing Game Exploits", List.of("exploit", "glitch", "dupe"),
            "Exploiting bugs or unintended mechanics for personal gain undermines fair gameplay and is not allowed.",
            "I acknowledge the use of exploits and will report issues rather than abuse them.",
            Material.END_CRYSTAL, false),

    META_GAMING("Meta Gaming", List.of("meta gaming", "meta", "ghosting", "ghost"),
            "Using out-of-character knowledge or communication to influence in-character gameplay, such as sharing player locations or plans, is prohibited.",
            "I acknowledge that Meta Gaming negatively impacts fair play and agree to avoid such behavior.",
            Material.SPECTRAL_ARROW, false),

    FALSE_REPORTS("Abuse of Reporting System", List.of("false report", "fake report"),
            "Filing false, malicious, or misleading reports wastes staff time and disrupts server operations.",
            "I understand that false reporting is a serious offense and will use the system appropriately.",
            Material.WRITABLE_BOOK, false),

    MINI_MODDING("Unauthorized Moderation", List.of("mini-mod", "self moderation"),
            "Players are not permitted to act in a staff role or attempt to moderate others. Please report violations through the proper channels.",
            "I acknowledge that I overstepped my role and will report rather than police others.",
            Material.LEAD, false),

    FOREIGN_LANGUAGE("Non-English Chat", List.of("foreign", "language", "non-english"),
            "To ensure inclusive communication, public chat must remain in English. Private messages may be in other languages.",
            "I understand that public chat must be in English and will adhere to this policy.",
            Material.GLOBE_BANNER_PATTERN, false),

    COMBAT_LOGGING("Combat Logging", List.of("combat log", "disconnect during fight"),
            "Disconnecting mid-combat to avoid in-game consequences is unfair and against the rules.",
            "I acknowledge logging out during combat and will remain in the game during fights going forward.",
            Material.TOTEM_OF_UNDYING, false),

    POLITICAL_CONTENT("Political Discussion", List.of("politics", "political talk"),
            "Political discussions and references are not permitted in public areas to maintain a neutral, welcoming space.",
            "I accept that political discussion is not appropriate and will avoid these topics on the server.",
            Material.WRITTEN_BOOK, false),

    STAFF_IMPERSONATION("Impersonating Staff", List.of("impersonation", "fake staff"),
            "Pretending to be or implying you are a staff member is strictly prohibited and may result in permanent removal.",
            "I acknowledge that impersonating staff is a violation of the rules and will not misrepresent myself again.",
            Material.NAME_TAG, false),

    TEMP_AUTOMATED("Automated Action – Pending Review", List.of("auto", "flagged", "temp action", "auto mod", "automated system"),
            "This action was taken automatically by our moderation systems due to flagged behavior. A staff member will review the incident to determine if further action is necessary.",
            "I understand that this action was automated and will await staff review for clarification or resolution.",
            Material.REDSTONE_TORCH, true);

    private final String displayName;
    private final List<String> aliases;
    private final String description;
    private final String affirmation;
    private final Material material;
    private final boolean hidden;

    InfractionType(String displayName, List<String> aliases, String description, String affirmation, Material material, boolean hidden) {
        this.displayName = displayName;
        this.aliases = aliases;
        this.description = description;
        this.affirmation = affirmation;
        this.material = material;
        this.hidden = hidden;
    }

    public static InfractionType[] visibleValues() {
        return Arrays.stream(values())
                .filter(type -> !type.isHidden())
                .toArray(InfractionType[]::new);
    }

    public List<RestrictionAction> getRecommendedActions() {
        return switch (this) {
            case PROFANITY, SPAM, DISRESPECT, FOREIGN_LANGUAGE -> List.of(
                    RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 30 * 60 * 1000L)
            );
            case ADVERTISING, DISRUPTION, HARASSMENT, FALSE_REPORTS, MINI_MODDING, POLITICAL_CONTENT -> List.of(
                    RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 6 * 60 * 60 * 1000L),
                    RestrictionAction.temporary(PunishmentType.WARN, 0)
            );
            case INAPPROPRIATE_CONTENT, COMBAT_LOGGING -> List.of(
                    RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 24 * 60 * 60 * 1000L),
                    RestrictionAction.temporary(PunishmentType.SUSPENSION, 24 * 60 * 60 * 1000L)
            );
            case HATE_SPEECH, STAFF_IMPERSONATION -> List.of(
                    RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 7 * 24 * 60 * 60 * 1000L),
                    RestrictionAction.temporary(PunishmentType.SUSPENSION, 7 * 24 * 60 * 60 * 1000L)
            );
            case UNAUTHORIZED_MODS, EXPLOITING -> List.of(
                    RestrictionAction.temporary(PunishmentType.SUSPENSION, 7 * 24 * 60 * 60 * 1000L)
            );
            case META_GAMING -> List.of(
                    RestrictionAction.temporary(PunishmentType.WARN, 0),
                    RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 2 * 60 * 60 * 1000L)
            );
            case TEMP_AUTOMATED -> List.of(
                    RestrictionAction.temporary(PunishmentType.CHAT_RESTRICTION, 24 * 60 * 60 * 1000L)
            );
        };
    }
}